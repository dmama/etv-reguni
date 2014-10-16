package ch.vd.uniregctb.evenement.civil.interne.annulation.deces;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Modélise un événement de décès.
 *
 * @author Ludovic BERTIN
 */
public class AnnulationDeces extends EvenementCivilInterne {

	protected static Logger LOGGER = LoggerFactory.getLogger(AnnulationDeces.class);

	/**
	 * Le conjoint Survivant.
	 */
	private Individu conjointSurvivant;

	protected AnnulationDeces(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		conjointSurvivant = context.getServiceCivil().getConjoint(evenement.getNumeroIndividuPrincipal(), evenement.getDateEvenement().getOneDayBefore());
	}

	protected AnnulationDeces(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		conjointSurvivant = context.getServiceCivil().getConjoint(evenement.getNumeroIndividu(), evenement.getDateEvenement().getOneDayBefore());
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationDeces(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
		this.conjointSurvivant = conjoint;
	}

	public Individu getConjointSurvivant() {

		final EtatCivil etatCivil = getIndividu().getEtatCivilCourant();

		// [UNIREG-1190] on n'expose pas le conjoint dans l'état-civil séparé (pas de différence avec le divorce au niveau fiscal)
		if (EtatCivilHelper.estMarieOuPacse(etatCivil)) {
			return conjointSurvivant;
		}
		else {
			return null;
		}
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		/*
		 * Obtention du tiers correspondant à l'ancient defunt.
		 */
		PersonnePhysique defunt = getPrincipalPP();

		/*
		 * Deux cas de figure :
		 * - il y a un conjoint survivant (conjoint fiscalement parlant)
		 * - il n'y a pas de conjoint survivant (conjoint fiscalement parlant)
		 */
		if (getConjointSurvivant() != null) {

			/*
			 * Obtention du tiers correspondant au veuf.
			 */
			PersonnePhysique veuf = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getConjointSurvivant().getNoTechnique());

			/*
			 * Récupération de l'ensemble decede-veuf-menageCommun
			 */
			EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(defunt, getDate());

			/*
			 * Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
			 */
			if (menageComplet == null || menageComplet.getMenage() == null) {
				throw new EvenementCivilException("Le tiers ménage commun n'a pu être trouvé");
			}

			/*
			 * Vérification de la cohérence
			 */
			if (!menageComplet.estComposeDe(defunt, veuf)) {
				throw new EvenementCivilException(
						"Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
			}
		}
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		/*
		 * Obtention du tiers correspondant a l'ancient defunt.
		 */
		PersonnePhysique defunt = getPrincipalPP();

		try {
			context.getMetierService().annuleDeces(defunt, getDate());

			// [SIFISC-6841] on met-à-jour le flag habitant en fonction de ses adresses de résidence civiles
			updateHabitantStatus(defunt, getDate());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return HandleStatus.TRAITE;
	}
}
