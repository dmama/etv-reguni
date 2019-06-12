package ch.vd.unireg.evenement.civil.interne.deces;

import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.shared.validation.ValidationResults;
import ch.vd.unireg.common.EtatCivilHelper;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilContext;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.evenement.civil.interne.HandleStatus;
import ch.vd.unireg.evenement.civil.regpp.EvenementCivilRegPP;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.metier.MetierServiceException;
import ch.vd.unireg.tiers.Contribuable;
import ch.vd.unireg.tiers.EnsembleTiersCouple;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.ForFiscalPrincipal;
import ch.vd.unireg.tiers.MenageCommun;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.type.MotifFor;
import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Modélise un événement de décès.
 *
 * @author Ludovic BERTIN
 */
public class Deces extends EvenementCivilInterne {

	protected static Logger LOGGER = LoggerFactory.getLogger(Deces.class);

	private final boolean fromRcpers;
	private boolean redondantSelonFors = false;

	/**
	 * Le conjoint Survivant.
	 */
	private Individu conjointSurvivant;


	protected Deces(EvenementCivilRegPP evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
		super(evenement, context, options);
		conjointSurvivant = context.getServiceCivil().getConjoint(evenement.getNumeroIndividuPrincipal(), evenement.getDateEvenement().getOneDayBefore());
		this.fromRcpers = false;
	}

	protected Deces(EvenementCivilEchFacade evenement, EvenementCivilContext context, EvenementCivilOptions options) throws EvenementCivilException {
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

		// [SIFISC-13311] Nous avons ici une suspicion d'absence de date de décès dans les données de l'individu... curieux, hein ?
		final Individu individuDefunt = getIndividu();
		if (individuDefunt.getDateDeces() == null) {
			erreurs.addErreur("La date de décès dans les données renvoyées par le registre civil est nulle.");
		}

		/*
		 * Obtention du tiers correspondant au defunt.
		 */
		PersonnePhysique defunt = getPrincipalPP();

		//presence d'une décision ACI
		verifierPresenceDecisionEnCours(defunt,getDate());
		verifierPresenceDecisionsEnCoursSurCouple(defunt);

		/*
		 * Deux cas de figure :
		 * - il y a un conjoint survivant (conjoint fiscalement parlant)
		 * - il n'y a pas de conjoint survivant (conjoint fiscalement parlant)
		 */
		if (getConjointSurvivant() != null) {

			/*
			 * Obtention du tiers correspondant au veuf.
			 */
			PersonnePhysique veuf = context.getTiersService().getPersonnePhysiqueByNumeroIndividu(getConjointSurvivant().getNoTechnique(), true);

			verifierPresenceDecisionEnCours(veuf,defunt,getDate());

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
			 */
			if (fromRcpers) {
				final ForFiscalPrincipal ffpMenage = menage.getForFiscalPrincipalAt(getDate());
				final ForFiscalPrincipal ffpDefunt = defunt.getForFiscalPrincipalAt(getDate().getOneDayAfter());
				if (ffpMenage != null && ffpMenage.getMotifFermeture() == MotifFor.VEUVAGE_DECES && getDate().equals(ffpMenage.getDateFin())) {
					// L'événement est redondant si le for fiscal principal du couple est fermé à la date de déces avec un motif veuvage décés
					redondantSelonFors = true;

					if (ffpDefunt != null && ffpDefunt.getMotifOuverture() == MotifFor.VEUVAGE_DECES && getDate().getOneDayAfter().equals(ffpDefunt.getDateDebut())) {
					// sauf dans le cas ou le défunt est veuf depuis le lendemain du jour de son déces ( on est dans le cas ou les 2 conjoints sont mort le même jour)
						redondantSelonFors = false;
					}

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
		try {
			return handleDeces(warnings);
		}
		finally {
			// [SIFISC-6841] on met-à-jour le flag habitant en fonction de ses adresses de résidence civiles
			updateHabitantStatus(getPrincipalPP(), getDate());
		}
	}

	private HandleStatus handleDeces(EvenementCivilWarningCollector warnings) throws EvenementCivilException {

		/*
		 * Obtention du tiers correspondant au defunt.
		 */
		final PersonnePhysique defunt = getPrincipalPP();

		// [UNIREG-775]
		final RegDate dateDecesUnireg = defunt.getDateDeces();
		if (dateDecesUnireg != null) { // si le décès a déjà été effectué dans UNIREG

			if (dateDecesUnireg.equals(getDate())) {
				// si l'evt civil de Décès est identique à la date de décès dans UNIREG : OK (evt traité sans modif dans UNIREG)
				context.audit.info(getNumeroEvenement(), "Date de décès déjà enregistrée dans le fiscal, rien à faire");
				checkForsFermesAvecPassageToutDroit(warnings);
				return redondantSelonFors ? HandleStatus.REDONDANT : HandleStatus.TRAITE;
			}
			else {

				final boolean unJourDifference = RegDateHelper.isBetween(getDate(), dateDecesUnireg.getOneDayBefore(), dateDecesUnireg.getOneDayAfter(), NullDateBehavior.EARLIEST);
				if (unJourDifference && dateDecesUnireg.year() == getDate().year()) {
					// si 1 jour de différence dans la même Période Fiscale (même année) : OK (evt traité sans modif dans UNIREG)
					context.audit.info(getNumeroEvenement(), "Date de décès déjà enregistrée dans le fiscal avec un jour de différence (" + RegDateHelper.dateToDisplayString(dateDecesUnireg) + "), rien à faire");
					checkForsFermesAvecPassageToutDroit(warnings);
					return redondantSelonFors ? HandleStatus.REDONDANT : HandleStatus.TRAITE;
				}
				else if (!unJourDifference || dateDecesUnireg.year() != getDate().year()) {
					// si plus d'1 jour d'écart ou sur une PF différente : KO (evt en Erreur --> pour traitement par la Cellule vérif de la date de décès)
					throw new EvenementCivilException("La date de décès diffère de celle dans le fiscal");
				}
			}
		}

		try {
			if (!redondantSelonFors) {
				context.getMetierService().deces(defunt, getDate(), null, getNumeroEvenement());
			}
			else {
				// les rapports sont ils tous fermés?
				// Va permettre de fermer tous les rapports restants. Si aucun rapport ouvert, alors pas de modification apportée
				// [SIFISC-25653] on ferme tous les rapports sauf les rapports d'héritage (côté "défunt")
				context.getTiersService().closeAllRapports(defunt, getDate(), r -> r.getType() == TypeRapportEntreTiers.HERITAGE && Objects.equals(r.getObjetId(), defunt.getNumero()));
			}
		}
		catch (MetierServiceException e) {
			throw new EvenementCivilException(e.getMessage(), e);
		}
		return HandleStatus.TRAITE;
	}

	/**
	 * On vérifie que les fors fiscaux sont bien fermés (ce qui n'est que justice pour une personne décédée) même dans le cas où Unireg ne fait rien pour l'événement civil de décès
	 * (car il est supposé déjà avoir été pris en compte, puisque la date de décès est déjà renseignée)
	 * @param warnings collection à remplir d'éventuels warnings
	 * @throws EvenementCivilException en cas de grave problème qui sera transcrit en erreur sur l'événement civil
	 */
	private void checkForsFermesAvecPassageToutDroit(EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		final PersonnePhysique defunt = getPrincipalPP();
		final EnsembleTiersCouple couple = getService().getEnsembleTiersCouple(defunt, getDate());
		final Contribuable ctbPourFors;
		if (couple != null && couple.getMenage() != null) {
			ctbPourFors = couple.getMenage();
		}
		else {
			ctbPourFors = defunt;
		}

		final List<ForFiscal> forsFiscauxOuverts = ctbPourFors.getForsFiscauxValidAt(null);
		if (forsFiscauxOuverts.size() > 0) {
			// visiblement, il y a encore des fors fiscaux ouverts, ce qui fait un peu tâche sur un décédé
			warnings.addWarning(String.format("Il reste au moins un for fiscal ouvert sur le contribuable %s malgré la date de décès déjà renseignée sur la personne physique %s.",
			                                  FormatNumeroHelper.numeroCTBToDisplay(ctbPourFors.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(defunt.getNumero())));
		}
	}
}
