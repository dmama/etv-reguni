package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;

/**
 * Stratégie de construction des événements civils internes issus d'événements civils eCH de correction qui
 * traite par une simple indexation les corrections sans impact fiscal et laisse en traitement manuel les autres
 */
public class DefaultCorrectionTranslationStrategy implements EvenementCivilEchTranslationStrategy {

	private static final EvenementCivilEchTranslationStrategy INDEXATION_PURE = new IndexationPureTranslationStrategy();
	private static final EvenementCivilEchTranslationStrategy TRAITEMENT_MANUEL = new TraitementManuelTranslationStrategy();

	private static final String MESSAGE_INDEXATION_PURE = "Evénement ignoré car sans impact fiscal.";
	private static final String SEPARATEUR = ", ";

	private final ServiceCivilService serviceCivil;
	private final List<IndividuComparisonStrategy> comparisonStrategies;

	public DefaultCorrectionTranslationStrategy(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
		this.comparisonStrategies = buildStrategies();
	}

	private static List<IndividuComparisonStrategy> buildStrategies() {
		// d'après la spécification, les éléments suivants doivent être comparés :
		// - date de naissance
		// - date de décès
		// - relations (filiations ou conjoints)
		// - état civil
		// - nationalité
		// - permis de séjour
		// - adresses de résidence (principale ou secondaire : date et EGID)
		final List<IndividuComparisonStrategy> strategies = new ArrayList<IndividuComparisonStrategy>();
		strategies.add(new DateNaissanceComparisonStrategy());
		strategies.add(new DateDecesComparisonStrategy());
		strategies.add(new RelationsComparisonStrategy());
		strategies.add(new EtatCivilComparisonStrategy());
		strategies.add(new NationaliteComparisonStrategy());
		strategies.add(new PermisComparisonStrategy());
		strategies.add(new AdresseResidenceComparisonStrategy());
		return strategies;
	}

	@Override
	public EvenementCivilInterne create(EvenementCivilEch event, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {

		if (event.getAction() != ActionEvenementCivilEch.CORRECTION) {
			throw new IllegalArgumentException("Stratégie applicable aux seuls événements civils de correction.");
		}

		final IndividuApresEvenement correction = serviceCivil.getIndividuFromEvent(event.getId());

		// on va comparer les individus avant et après correction,
		final Long idEvtOriginel;
		if (event.getRefMessageId() != null) {
			idEvtOriginel = event.getRefMessageId();
		}
		else {
			idEvtOriginel = correction.getIdEvenementRef();
			event.setRefMessageId(idEvtOriginel);       // contournement du bug SIREF-2590 (l'id de référence n'était pas présent dans l'événement initial)
		}
		if (idEvtOriginel == null) {
			throw new EvenementCivilException("Impossible de traiter un événement civil de correction sans lien vers l'événement originel.");
		}

		final IndividuApresEvenement originel = serviceCivil.getIndividuFromEvent(idEvtOriginel);
		final DataHolder<String> message = new DataHolder<String>();
		final EvenementCivilEchTranslationStrategy strategieApplicable;
		if (sansDifferenceFiscalementImportante(originel, correction, message)) {
			strategieApplicable = INDEXATION_PURE;
			if (StringUtils.isBlank(message.get())) {
				event.setCommentaireTraitement(MESSAGE_INDEXATION_PURE);
			}
		}
		else {
			// il y a des différences... on ne peut rien faire automatiquement -> traitement manuel.
			strategieApplicable = TRAITEMENT_MANUEL;
		}

		// éventuelle explication sur la différence blocante observée
		if (StringUtils.isNotBlank(message.get())) {
			final String commentaire;
			if (message.get().contains(SEPARATEUR)) {
				commentaire = String.format("Les éléments suivants ont été modifiés par la correction : %s.", message.get());
			}
			else {
				commentaire = String.format("L'élément suivant a été modifié par la correction : %s.", message.get());
			}
			event.setCommentaireTraitement(commentaire);
		}

		return strategieApplicable.create(event, context, options);
	}

	@Override
	public boolean isPrincipalementIndexation(EvenementCivilEch event, EvenementCivilContext context) throws EvenementCivilException {
		return false;
	}

	private boolean sansDifferenceFiscalementImportante(IndividuApresEvenement originel, IndividuApresEvenement correction, @NotNull DataHolder<String> msg) {

		// si un des individus manque, alors il y a forcément des différences importantes
		if (originel == null || correction == null || originel.getIndividu() == null || correction.getIndividu() == null) {
			msg.set("individu");
			return false;
		}

		final StringBuilder b = new StringBuilder();
		boolean sans = true;
		for (IndividuComparisonStrategy strategy : comparisonStrategies) {
			final DataHolder<String> localMsg = new DataHolder<String>();
			sans = strategy.sansDifferenceFiscalementImportante(originel, correction, localMsg) && sans;
			if (StringUtils.isNotBlank(localMsg.get())) {
				if (b.length() > 0) {
					b.append(SEPARATEUR);
				}
				b.append(localMsg.get());
			}
		}
		msg.set(b.length() > 0 ? b.toString() : null);
		return sans;
	}
}
