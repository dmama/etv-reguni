package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;

/**
 * Stratégie de construction des événements civils internes issus d'événements civils eCH de correction qui
 * traite par une simple indexation les corrections sans impact fiscal et laisse en traitement manuel les autres
 */
public class DefaultCorrectionTranslationStrategy implements EvenementCivilEchTranslationStrategy {

	private static final EvenementCivilEchTranslationStrategy INDEXATION_PURE = new IndexationPureTranslationStrategy();
	private static final EvenementCivilEchTranslationStrategy TRAITEMENT_MANUEL = new TraitementManuelTranslationStrategy();

	private static final String MESSAGE_ANCIEN_HABITANT = "Evénement civil de correction sur un ancien habitant du canton.";
	private static final String SEPARATEUR = ", ";

	private final ServiceCivilService serviceCivil;
	private final TiersService tiersService;
	private final List<IndividuComparisonStrategy> comparisonStrategies;

	public DefaultCorrectionTranslationStrategy(ServiceCivilService serviceCivil, ServiceInfrastructureService serviceInfrastructureService, TiersService tiersService) {
		this.serviceCivil = serviceCivil;
		this.tiersService = tiersService;
		this.comparisonStrategies = buildStrategies(serviceInfrastructureService);
	}

	private static List<IndividuComparisonStrategy> buildStrategies(ServiceInfrastructureService serviceInfrastructureService) {
		final List<IndividuComparisonStrategy> strategies = new ArrayList<IndividuComparisonStrategy>();
		strategies.add(new AdresseContactComparisonStrategy());
		strategies.add(new AdresseResidencePrincipaleComparisonStrategy(serviceInfrastructureService));
		strategies.add(new AdresseResidenceSecondaireComparisonStrategy(serviceInfrastructureService));
		strategies.add(new DateDecesComparisonStrategy());
		strategies.add(new DateNaissanceComparisonStrategy());
		strategies.add(new EtatCivilComparisonStrategy());
		strategies.add(new NationaliteComparisonStrategy());
		strategies.add(new PermisComparisonStrategy());
		strategies.add(new RelationsComparisonStrategy());
//		strategies.add(new SexeComparisonStrategy());       // finalement enlevé....
		return strategies;
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		if (event.getAction() != ActionEvenementCivilEch.CORRECTION) {
			throw new IllegalArgumentException("Stratégie applicable aux seuls événements civils de correction.");
		}

		final EvenementCivilEchTranslationStrategy strategieApplicable;
		if (isContribuableAncienHabitant(event.getNumeroIndividu())) {
			// il faudrait reprendre les données qui ont changé du civil, tout en n'écrasant pas les données changées directement changées dans Unireg...
			event.setCommentaireTraitement(MESSAGE_ANCIEN_HABITANT);
			strategieApplicable = TRAITEMENT_MANUEL;
		}
		else {

			// on va comparer les individus avant et après correction,
			final Long idEvtOriginel = event.getRefMessageId();
			if (idEvtOriginel == null) {
				throw new EvenementCivilException("Impossible de traiter un événement civil de correction sans lien vers l'événement originel.");
			}

			final IndividuApresEvenement originel = serviceCivil.getIndividuAfterEvent(idEvtOriginel);
			if (originel == null) {
				throw new EvenementCivilException(String.format("Impossible d'obtenir les données de l'événement civil %d corrigé", idEvtOriginel));
			}

			final IndividuApresEvenement correction = serviceCivil.getIndividuAfterEvent(event.getId());
			if (correction == null) {
				throw new EvenementCivilException(String.format("Impossible d'obtenir les données de l'événement civil %d de correction", event.getId()));
			}

			strategieApplicable = getStrategyBasedOnDifferences(event, originel, correction);
		}

		// un peu de log pour pouvoir suivre après coup ce qui s'est passé
		if (StringUtils.isNotBlank(event.getCommentaireTraitement())) {
			Audit.info(event.getId(), event.getCommentaireTraitement());
		}

		return strategieApplicable.create(event, context, options);
	}

	private boolean isContribuableAncienHabitant(long noIndividu) {
		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		return pp != null && !pp.isHabitantVD();
	}

	private EvenementCivilEchTranslationStrategy getStrategyBasedOnDifferences(EvenementCivilEch event, IndividuApresEvenement originel, IndividuApresEvenement correction) {
		final List<String> champsModifies = new LinkedList<String>();
		final EvenementCivilEchTranslationStrategy strategieApplicable;
		if (isFiscalementNeutre(originel, correction, champsModifies)) {
			strategieApplicable = INDEXATION_PURE;
		}
		else {
			// il y a des différences... on ne peut rien faire automatiquement -> traitement manuel.
			strategieApplicable = TRAITEMENT_MANUEL;
		}

		// éventuelle explication sur la différence blocante observée
		if (champsModifies.size() > 0) {
			final String commentaire;
			if (champsModifies.size() > 1) {
				commentaire = String.format("Les éléments suivants ont été modifiés par la correction : %s.", toString(champsModifies));
			}
			else {
				commentaire = String.format("L'élément suivant a été modifié par la correction : %s.", champsModifies.get(0));
			}
			event.setCommentaireTraitement(commentaire);
		}

		return strategieApplicable;
	}

	private static String toString(List<String> champsModifies) {
		final StringBuilder b = new StringBuilder();
		for (String s : champsModifies) {
			if (b.length() > 0) {
				b.append(SEPARATEUR);
			}
			b.append(s);
		}
		return b.toString();
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		return false;
	}

	private boolean isFiscalementNeutre(@NotNull IndividuApresEvenement originel, @NotNull IndividuApresEvenement correction, @NotNull List<String> champsModifies) {

		// si un des individus manque, alors il y a forcément des différences importantes
		if (originel.getIndividu() == null || correction.getIndividu() == null) {
			champsModifies.add("individu");
			return false;
		}

		boolean neutre = true;
		for (IndividuComparisonStrategy strategy : comparisonStrategies) {
			final DataHolder<String> champ = new DataHolder<String>();
			neutre = strategy.isFiscalementNeutre(originel, correction, champ) && neutre;
			if (StringUtils.isNotBlank(champ.get())) {
				champsModifies.add(champ.get());
			}
		}
		return neutre;
	}
}
