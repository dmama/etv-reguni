package ch.vd.uniregctb.evenement.civil.interne.deces;

import org.apache.log4j.Logger;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.Pair;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.externe.EvenementCivilExterne;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;

/**
 * Modélise un événement de décès.
 *
 * @author Ludovic BERTIN
 */
public class Deces extends EvenementCivilInterne {

	protected static Logger LOGGER = Logger.getLogger(Deces.class);

	/**
	 * Le conjoint Survivant.
	 */
	private Individu conjointSurvivant;

	protected Deces(EvenementCivilExterne evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		conjointSurvivant = context.getServiceCivil().getConjoint(evenement.getNumeroIndividuPrincipal(), evenement.getDateEvenement().getOneDayBefore());
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Deces(Individu individu, Long principalPPId, Individu conjoint, Long conjointPPId, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, principalPPId, conjoint, conjointPPId, date, numeroOfsCommuneAnnonce, context);
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
	public void validateSpecific(EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		// Aucune validation spécifique

		/*
		 * Obtention du tiers correspondant au defunt.
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
			PersonnePhysique veuf = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getConjointSurvivant().getNoTechnique() );

			/*
			 * Récupération de l'ensemble decede-veuf-menageCommun
			 */
			EnsembleTiersCouple menageComplet = context.getTiersService().getEnsembleTiersCouple(defunt, getDate());

			/*
			 * Vérification de la cohérence
			 */
			if (menageComplet == null) {
				throw new EvenementCivilException("L'individu est marié ou en partenariat enregistré mais ne possède pas de ménage commun");
			}
			if (!menageComplet.estComposeDe(defunt, veuf)) {
				throw new EvenementCivilException(
						"Les tiers composant le tiers ménage trouvé ne correspondent pas avec les individus unis dans le civil");
			}

			/*
			 * On récupère le tiers MenageCommun
			 */
			MenageCommun menage = menageComplet.getMenage();

			/*
			 * Si le tiers MenageCommun n'est pas trouvé, la base fiscale est inconsistente => mise en erreur de l'événement
			 */
			if (menage == null) {
				throw new EvenementCivilException("Le tiers ménage commun n'a pu être trouvé");
			}
		}

		/*
		 * Validations métier
		 */
		ValidationResults validationResults = context.getMetierService().validateDeces(defunt, getDate());
		addValidationResults(erreurs, warnings, validationResults);
	}

	@Override
	public Pair<PersonnePhysique, PersonnePhysique> handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		/*
		 * Obtention du tiers correspondant au defunt.
		 */
		final PersonnePhysique defunt = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getNoIndividu());

		// [UNIREG-775]
		final RegDate dateDecesUnireg = defunt.getDateDeces();
		if (dateDecesUnireg != null) { // si le décès a déjà été effectué dans UNIREG

			if (dateDecesUnireg.equals(getDate())) {
				// si l'evt civil de Décès est identique à la date de décès dans UNIREG : OK (evt traité sans modif dans UNIREG)

				// [UNIREG-2653] Mais il faut tout de même passer l'individu en "non-habitant"
				if (defunt.isHabitantVD()) {
					context.getTiersService().changeHabitantenNH(defunt);
				}
				return null;
			}
			else {

				final boolean unJourDifference = RegDateHelper.isBetween(getDate(), dateDecesUnireg.getOneDayBefore(), dateDecesUnireg.getOneDayAfter(), NullDateBehavior.EARLIEST);
				if (unJourDifference && dateDecesUnireg.year() == getDate().year()) {
					// si 1 jour de différence dans la même Période Fiscale (même année) : OK (evt traité sans modif dans UNIREG)

					// [UNIREG-2653] Mais il faut tout de même passer l'individu en "non-habitant"
					if (defunt.isHabitantVD()) {
						context.getTiersService().changeHabitantenNH(defunt);
					}
					return null;
				}
				else if (!unJourDifference || dateDecesUnireg.year() != getDate().year()) {
					// si plus d'1 jour d'écart ou sur une PF différente : KO (evt en Erreur --> pour traitement par la Cellule vérif de la date de décès)
					throw new EvenementCivilException("La date de décès diffère de celle dans le fiscal");
				}
			}
		}

		try {
			context.getMetierService().deces(defunt, getDate(), null, getNumeroEvenement());
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return null;
	}
}
