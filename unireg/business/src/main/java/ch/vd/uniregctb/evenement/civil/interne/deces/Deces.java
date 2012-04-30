package ch.vd.uniregctb.evenement.civil.interne.deces;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.common.EtatCivilHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilContext;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.evenement.civil.interne.HandleStatus;
import ch.vd.uniregctb.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.type.MotifFor;

/**
 * Modélise un événement de décès.
 *
 * @author Ludovic BERTIN
 */
public class Deces extends EvenementCivilInterne {

	protected static Logger LOGGER = Logger.getLogger(Deces.class);

	private final boolean fromRcpers;
	private boolean isRedondant = false;

	/**
	 * Le conjoint Survivant.
	 */
	private Individu conjointSurvivant;


	protected Deces(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		conjointSurvivant = context.getServiceCivil().getConjoint(evenement.getNumeroIndividuPrincipal(), evenement.getDateEvenement().getOneDayBefore());
		this.fromRcpers = false;
	}

	protected Deces(EvenementCivilEch evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		conjointSurvivant = context.getServiceCivil().getConjoint(evenement.getNumeroIndividu(), evenement.getDateEvenement().getOneDayBefore());
		this.fromRcpers = true;
	}

	/**
	 * Pour le testing uniquement.
	 */
	@SuppressWarnings({"JavaDoc"})
	protected Deces(Individu individu, Individu conjoint, RegDate date, Integer numeroOfsCommuneAnnonce, EvenementCivilContext context) {
		super(individu, conjoint, date, numeroOfsCommuneAnnonce, context);
		this.conjointSurvivant = conjoint;
		this.fromRcpers = false;
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

		// Aucune validation spécifique

		/*
		 * Obtention du tiers correspondant au defunt.
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

			/*
			 * Détection de la redondance pour les evenements venant de Rcpers.
			 *    -> L'evenement de deces peut être redondant si l'evenement de veuvage de son conjoint a été traité avant
			 *    -> TODO FRED à tester le cas ou les 2 conjoints décédent le même jour
			 */
			if (fromRcpers) {
				final ForFiscalPrincipal ffpMenage = menage.getForFiscalPrincipalAt(getDate());
				if (ffpMenage != null && ffpMenage.getMotifFermeture() == MotifFor.VEUVAGE_DECES && getDate().equals(ffpMenage.getDateFin())) {
					// L'événement est redondant si le for fiscal principal du couple est fermé à la date de déces avec un motif veuvage décés
					isRedondant = true;
				}
			}
		}

		/*
		 * Validations métier
		 */
		ValidationResults validationResults = context.getMetierService().validateDeces(defunt, getDate());

		addValidationResults(erreurs, warnings, validationResults);
	}

	@NotNull
	@Override
	public HandleStatus handle(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		if (isRedondant) {
			return HandleStatus.REDONDANT;
		}

		/*
		 * Obtention du tiers correspondant au defunt.
		 */
		final PersonnePhysique defunt = getPrincipalPP();

		// [UNIREG-775]
		final RegDate dateDecesUnireg = defunt.getDateDeces();
		if (dateDecesUnireg != null) { // si le décès a déjà été effectué dans UNIREG

			if (dateDecesUnireg.equals(getDate())) {
				// si l'evt civil de Décès est identique à la date de décès dans UNIREG : OK (evt traité sans modif dans UNIREG)

				// [UNIREG-2653] Mais il faut tout de même passer l'individu en "non-habitant"
				if (defunt.isHabitantVD()) {
					context.getTiersService().changeHabitantenNH(defunt);
				}
				return HandleStatus.TRAITE;
			}
			else {

				final boolean unJourDifference = RegDateHelper.isBetween(getDate(), dateDecesUnireg.getOneDayBefore(), dateDecesUnireg.getOneDayAfter(), NullDateBehavior.EARLIEST);
				if (unJourDifference && dateDecesUnireg.year() == getDate().year()) {
					// si 1 jour de différence dans la même Période Fiscale (même année) : OK (evt traité sans modif dans UNIREG)

					// [UNIREG-2653] Mais il faut tout de même passer l'individu en "non-habitant"
					if (defunt.isHabitantVD()) {
						context.getTiersService().changeHabitantenNH(defunt);
					}
					return HandleStatus.TRAITE;
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
		return HandleStatus.TRAITE;
	}

}
