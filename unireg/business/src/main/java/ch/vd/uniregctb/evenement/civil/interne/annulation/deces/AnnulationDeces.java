package ch.vd.uniregctb.evenement.civil.interne.annulation.deces;

import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.utils.Pair;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilHandlerException;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterneErreur;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterneException;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Modélise un événement de décès.
 *
 * @author Ludovic BERTIN
 */
public class AnnulationDeces extends EvenementCivilInterne {

	protected static Logger LOGGER = Logger.getLogger(AnnulationDeces.class);

	/**
	 * Le conjoint Survivant.
	 */
	private Individu conjointSurvivant;

	protected AnnulationDeces(EvenementCivilExterne evenement, EvenementCivilContext context) throws EvenementCivilInterneException {
		super(evenement, context);
		conjointSurvivant = context.getServiceCivil().getConjoint(evenement.getNumeroIndividuPrincipal(), evenement.getDateEvenement().getOneDayBefore());
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationDeces(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, TypeEvenementCivil.ANNUL_DECES, date, numeroOfsCommuneAnnonce, context);
		this.conjointSurvivant = conjoint;
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected AnnulationDeces(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, TypeEvenementCivil.ANNUL_DECES, date, numeroOfsCommuneAnnonce, context);
		this.conjointSurvivant = conjoint;
	}

	public Individu getConjointSurvivant() {

		final TypeEtatCivil typeEtatCivil = getIndividu().getEtatCivilCourant().getTypeEtatCivil();

		// [UNIREG-1190] on n'expose pas le conjoint dans l'état-civil séparé (pas de différence avec le divorce au niveau fiscal)
		if (typeEtatCivil == TypeEtatCivil.CELIBATAIRE || typeEtatCivil == TypeEtatCivil.DIVORCE || typeEtatCivil == TypeEtatCivil.PACS_ANNULE ||
				typeEtatCivil == TypeEtatCivil.PACS_INTERROMPU || typeEtatCivil == TypeEtatCivil.VEUF || typeEtatCivil == TypeEtatCivil.SEPARE) {
			return null;
		}

		return conjointSurvivant;
	}

	@Override
	protected boolean forceRefreshCacheConjoint() {
		return true;
	}

	@Override
	public void checkCompleteness(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {
	}

	@Override
	public void validateSpecific(List<EvenementCivilExterneErreur> erreurs, List<EvenementCivilExterneErreur> warnings) {

		/*
		 * Obtention du tiers correspondant à l'ancient defunt.
		 */
		PersonnePhysique defunt = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());

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
				throw new EvenementCivilHandlerException("Le tiers ménage commun n'a pu être trouvé");
			}

			/*
			 * Vérification de la cohérence
			 */
			if (!menageComplet.estComposeDe(defunt, veuf)) {
				throw new EvenementCivilHandlerException(
						"Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
			}
		}
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(List<EvenementCivilExterneErreur> warnings) throws EvenementCivilHandlerException {

		/*
		 * Obtention du tiers correspondant a l'ancient defunt.
		 */
		PersonnePhysique defunt = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());

		context.getMetierService().annuleDeces(defunt, getDate());
		return null;
	}
}
