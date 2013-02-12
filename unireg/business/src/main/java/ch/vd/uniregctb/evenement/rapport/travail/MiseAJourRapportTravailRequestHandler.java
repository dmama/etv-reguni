package ch.vd.uniregctb.evenement.rapport.travail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.orm.hibernate3.HibernateTemplate;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.xml.Context;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;


public class MiseAJourRapportTravailRequestHandler implements RapportTravailRequestHandler {

	private static final Logger LOGGER = Logger.getLogger(MiseAJourRapportTravailRequestHandler.class);

	private TiersService tiersService;
	private final Context context = new Context();


	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setHibernateTemplate(HibernateTemplate template) {
		context.hibernateTemplate = template;
	}

	@Override
	public MiseAJourRapportTravailResponse handle(MiseAjourRapportTravail request) throws ServiceException, ValidationException {


		//Initialisation de la période de déclaration
		final DateRange periodeDeclaration = new DateRangeHelper.Range(request.getDateDebutPeriodeDeclaration(), request.getDateFinPeriodeDeclaration());
		final DebiteurPrestationImposable dpi = getDebiteur(request);

		// Le débiteur doit être actif (For en vigueur) sur toute la période de déclaration
		validateDebiteur(dpi, periodeDeclaration);

		final PersonnePhysique sourcier = getSourcier(request);


		//On retrouve le rapport a modifier.
		final List<RapportPrestationImposable> rapportsAModifier = findRapportPrestationImposable(dpi, sourcier);
		final List<RapportPrestationImposable> nouveauxRapports = new ArrayList<RapportPrestationImposable>();

		if (rapportsAModifier == null || rapportsAModifier.isEmpty()) {
			handleRapportPrestationInexistant(dpi, sourcier, request, nouveauxRapports);
		}
		else {
			for (RapportPrestationImposable rapportAModifier : rapportsAModifier) {
				handleRapportPrestationExistant(dpi, sourcier, rapportAModifier, request, nouveauxRapports);

			}

		}

		//Après l'application de l'algo général, on récupère la liste des rapports modifiés afin d'effectuer les deux derniers traitements
		final List<RapportPrestationImposable> rapportsModifies = getRapportPrestation(dpi, sourcier);
		traiterDateDeFinForDebiteur(dpi, request, rapportsModifies, nouveauxRapports);

		traiterChevauchementRapport(dpi, sourcier, request, rapportsModifies, nouveauxRapports);

		//Ajout des rapports calculés après élimination des doublons et des chevauchements.
		addNewRapport(dpi,sourcier,nouveauxRapports);

		return createResponse(request);
	}

	private void addNewRapport(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, List<RapportPrestationImposable> nouveauxRapports) {
		for (RapportPrestationImposable nouveauxRapport : nouveauxRapports) {
			tiersService.addRapportPrestationImposable(sourcier, dpi, nouveauxRapport.getDateDebut(), nouveauxRapport.getDateFin());
		}
	}

	List<RapportPrestationImposable> getRapportPrestation(DebiteurPrestationImposable dpi, PersonnePhysique sourcier) {
		final List<RapportPrestationImposable> rapports = new ArrayList<RapportPrestationImposable>();
		for (RapportEntreTiers rapportEntreTiers : dpi.getRapportsObjet()) {
			if (rapportEntreTiers instanceof RapportPrestationImposable && rapportEntreTiers.getSujetId().equals(sourcier.getId()) && !rapportEntreTiers.isAnnule()) {
				rapports.add((RapportPrestationImposable) rapportEntreTiers);
			}
		}
		return rapports;
	}

	/**
	 * Ferme les rapports de travail encore ouverts yi le dernier for du dpi est fermé à la date de fin de période
	 *
	 * @param dpi              le debiteur
	 * @param request          la demande
	 * @param nouveauxRapports
	 */
	private void traiterDateDeFinForDebiteur(DebiteurPrestationImposable dpi, MiseAjourRapportTravail request, List<RapportPrestationImposable> rapportAModifier,
	                                         List<RapportPrestationImposable> nouveauxRapports) {
		if (isDernierForFiscalFermeFinPeriode(dpi, request)) {
			//on ferme tous les rapports encore ouverts a cette date la
			for (RapportPrestationImposable rapportPrestationImposable : rapportAModifier) {

				if (rapportPrestationImposable.getDateFin() == null) {
					//la date de fin du for est la même que la de fin de période
					fermerRapportTravail(rapportPrestationImposable, request.getDateFinPeriodeDeclaration());
				}
			}
			//on ferme tous les rapports encore ouverts a cette date la dans la liste des nouveaux rapports
			for (RapportPrestationImposable rapportPrestationImposable : nouveauxRapports) {

				if (rapportPrestationImposable.getDateFin() == null) {
					//la date de fin du for est la même que la de fin de période
					fermerRapportTravail(rapportPrestationImposable, request.getDateFinPeriodeDeclaration());
				}
			}


		}

	}

	/**
	 * Determine si un debiteur a son dernier for fiscal ferme à la date de fin de la periode de déclaration
	 *
	 * @param dpi
	 * @param request
	 * @return<b>vrai</b> si le dernier for est fermé à la date de fin de la période, <b>faux</b> sinon
	 */
	private boolean isDernierForFiscalFermeFinPeriode(DebiteurPrestationImposable dpi, MiseAjourRapportTravail request) {
		final RegDate dateFinPeriodeDeclaration = request.getDateFinPeriodeDeclaration();
		ForFiscal forFiscal = dpi.getForDebiteurPrestationImposableAt(dateFinPeriodeDeclaration);
		if (forFiscal != null) {
			final RegDate dateFinFor = forFiscal.getDateFin();
			//Meme date de fin et aucun autre for ouvert après
			if (dateFinFor == dateFinPeriodeDeclaration && dpi.getForDebiteurPrestationImposableAfter(dateFinFor) == null) {
				return true;
			}
		}
		else {
			//Pas de for Fiscal à cette période, c'est assez bizarre, à logger
			String message = String.format("Pas de for fiscal trouvé pour la fin de la période de déclaration %s:" +
					" concernant le débiteur %s. Il y a peut être une incohérence entre le contenu du message %s et les données du débiteur dans unireg",
					RegDateHelper.dateToDisplayString(request.getDateFinPeriodeDeclaration()),
					FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()),
					request.getBusinessId());
			LOGGER.info(message);
		}
		return false;
	}

	private void traiterChevauchementRapport(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, MiseAjourRapportTravail request, List<RapportPrestationImposable> rapportsAModifier,
	                                         List<RapportPrestationImposable> nouveauxRapports) {

		final DateRange periodeDeclaration = new DateRangeHelper.Range(request.getDateDebutPeriodeDeclaration(), request.getDateFinPeriodeDeclaration());

		//final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi,sourcier, true, true);
		final List<RapportPrestationImposable> rapportsConcernes = new ArrayList<RapportPrestationImposable>();
		for (RapportPrestationImposable rapport : rapportsAModifier) {
			if (DateRangeHelper.intersect(periodeDeclaration, rapport)) {
				rapportsConcernes.add(rapport);
			}
		}

		for (RapportPrestationImposable rapport : nouveauxRapports) {
			if (DateRangeHelper.intersect(periodeDeclaration, rapport)) {
				rapportsConcernes.add(rapport);
			}
		}

		if (rapportsConcernes.size() > 1) {

			//on orddonnes les rapports;
			Collections.sort(rapportsConcernes, new DateRangeComparator<RapportPrestationImposable>());

			//On prépare la date de début et la date de fin du rapport
			RegDate dateDebutNouveauRapport = rapportsConcernes.get(0).getDateDebut();
			final int lastRapportPos = rapportsConcernes.size() - 1;
			RegDate dateFinNouveauRapport = rapportsConcernes.get(lastRapportPos).getDateFin();

			// on créer un nouveau rapport que l'on ajoute dans la liste des rapports à ajouter en fin de traitement.
			final RapportPrestationImposable nouveauRapport = new RapportPrestationImposable(dateDebutNouveauRapport, dateFinNouveauRapport, sourcier, dpi);
			nouveauxRapports.add(nouveauRapport);
			//on annule touts les rapports précédents
			for (RapportPrestationImposable rapportsConcerne : rapportsConcernes) {
				//on annule touts les rapports précédents déja existant
				if (rapportsConcerne.getId() != null) {
					rapportsConcerne.setAnnule(true);
					final String message = String.format("Traitement des doublons et des chevauchements: Annulation du rapport commencant le  %s " +
							" et se terminant le %s pour le débiteur %s et le sourcier %s. Il sera remplacé par un nouveau rapport de prestation avec les dates de débuts et dates de fin adaptées",
							RegDateHelper.dateToDisplayString(rapportsConcerne.getDateDebut()),
							RegDateHelper.dateToDisplayString(rapportsConcerne.getDateFin()),
							FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()),
							FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()));
					LOGGER.info(message);
				}
				else {
					//on peut supprimer ce nouveau rapport de la liste des rapport à ajouter
					nouveauxRapports.remove(rapportsConcerne);
					final String message = String.format("Traitement des doublons et des chevauchements: Suppression du rapport temporaire calculé  commencant le  %s " +
							" et se terminant le %s pour le débiteur %s  et le sourcier %s. Il sera remplacé par un nouveau rapport de prestation avec les dates de débuts et dates de fin adaptées.",
							RegDateHelper.dateToDisplayString(rapportsConcerne.getDateDebut()),
							RegDateHelper.dateToDisplayString(rapportsConcerne.getDateFin()),
							FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()),
							FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()));
					LOGGER.info(message);
				}


			}

			String message = String.format("nouveau rapport de travail créé suite à la détection de chevauchement:" +
					"Ce nouveau rapport commence le %s et se termine le %s.  Concerne le debiteur %s et le sourcier %s.",
					RegDateHelper.dateToDisplayString(nouveauRapport.getDateDebut()),
					RegDateHelper.dateToDisplayString(nouveauRapport.getDateFin()),
					FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()),
					FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()));
			LOGGER.info(message);

		}
	}


	private PersonnePhysique getSourcier(MiseAjourRapportTravail request) throws ServiceException {
		//Le sourcier doit exister
		final long numeroSourcier = request.getIdSourcier();
		final Tiers tiers = tiersService.getTiers(numeroSourcier);

		if (tiers == null) {
			final String msg = String.format("le sourcier %s n'existe pas dans unireg", FormatNumeroHelper.numeroCTBToDisplay(numeroSourcier));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}

		if (!(tiers instanceof PersonnePhysique)) {
			final String msg = String.format("le sourcier %s ne correspond pas à une personne physique", FormatNumeroHelper.numeroCTBToDisplay(numeroSourcier));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.INVALID_PARTY_TYPE.name(), null));
		}

		final PersonnePhysique sourcier = (PersonnePhysique) tiers;
		return sourcier;
	}

	private DebiteurPrestationImposable getDebiteur(MiseAjourRapportTravail request) throws ServiceException {

		final long numeroDpi = request.getIdDebiteur();
		final Tiers tiers = tiersService.getTiers(numeroDpi);
		//le débiteur doit exister
		if (tiers == null) {
			final String msg = String.format("le débiteur %s n'existe pas dans unireg", FormatNumeroHelper.numeroCTBToDisplay(numeroDpi));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}

		//nature du tiers récupéré
		if (!(tiers instanceof DebiteurPrestationImposable)) {
			final String msg = String.format("le tiers %s ne correspond pas à un débiteur", FormatNumeroHelper.numeroCTBToDisplay(numeroDpi));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.INVALID_PARTY_TYPE.name(), null));
		}

		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiers;
		return dpi;
	}

	private void handleRapportPrestationExistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request,
	                                             List<RapportPrestationImposable> nouveauxRapports) {
		//On a un evenement de fermeture de rapport de travail
		if (request.isFermetureRapportTravail()) {
			handleEvenementFermetureRapportTravailExistant(rapportAModifier, request);

		}
		else {
			handleModificationRapportTravailExistant(dpi, sourcier, rapportAModifier, request, nouveauxRapports);
		}
	}

	private void handleModificationRapportTravailExistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request,
	                                                      List<RapportPrestationImposable> nouveauxRapports) {

		if (isRapportSurPeriode(rapportAModifier, request)) {
			handleRapportSurPeriode(rapportAModifier, request, nouveauxRapports);
		}
		else if (isRapportOuvertApresPeriode(rapportAModifier, request)) {
			handleRapportOuvertApresPeriode(rapportAModifier, request, nouveauxRapports);
		}
		else if (isRapportFermeAvantPeriodeDeclaration(rapportAModifier, request)) {
			handleRapportFermeAvantPeriodeDeclaration(dpi, sourcier, rapportAModifier, request, nouveauxRapports);
		}

	}

	private void handleRapportFermeAvantPeriodeDeclaration(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request,
	                                                       List<RapportPrestationImposable> nouveauxRapports) {
		final RegDate dateFin = rapportAModifier.getDateFin();
		final RegDate dateDebutVersement = request.getDateDebutVersementSalaire();

		if (isEcartInferieurEgalAUnJour(dateDebutVersement, dateFin)) {
			//On réouvre le rapport
			loggerModification("Reouverture ", rapportAModifier);
			final RapportPrestationImposable rapportPrestationImposable = new RapportPrestationImposable(rapportAModifier);
			rapportPrestationImposable.setDateFin(null);
			rapportAModifier.setAnnule(true);


			final RegDate nouvelleDateFin = calculerDateFinRapportTravail(request);
			if (nouvelleDateFin != null) {
				loggerModification("Fermeture ", rapportAModifier);
				rapportPrestationImposable.setDateFin(nouvelleDateFin);
			}
			nouveauxRapports.add(rapportPrestationImposable);
		}
		else {
			creerRapportTravail(dpi, sourcier, request, nouveauxRapports);

		}
	}

	private boolean isEcartInferieurEgalAUnJour(RegDate dateDebutPeriode, RegDate dateFin) {
		final RegDate veille = dateDebutPeriode.getOneDayBefore();
		final RegDate avantVeille = veille.getOneDayBefore();
		if (dateFin == veille || dateFin == avantVeille) {
			return true;
		}
		return false;
	}

	private void handleRapportOuvertApresPeriode(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request, List<RapportPrestationImposable> nouveauxRapports) {

		final RegDate dateDebutVersementSalaire = request.getDateDebutVersementSalaire();

		final String modification = String.format("Modification de la date de début, nouvelle date: %s ", dateDebutVersementSalaire);

		loggerModification(modification, rapportAModifier);

		final RapportPrestationImposable rapportPrestationImposable = new RapportPrestationImposable(rapportAModifier);
		rapportPrestationImposable.setDateDebut(dateDebutVersementSalaire);
		rapportAModifier.setAnnule(true);
		nouveauxRapports.add(rapportPrestationImposable);


	}

	private void loggerModification(String modification, RapportPrestationImposable rapportAModifier) {
		String message = String.format(modification + " pour le rapport de travail commencant" +
				" le %s pour le debiteur %s et le sourcier %s.",
				RegDateHelper.dateToDisplayString(rapportAModifier.getDateDebut()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getObjetId()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getSujetId()));
		LOGGER.info(message);
	}

	/**
	 * Applique les règles de modification de date de fin sur un rapport qui recoupe la periode de declaration du message
	 *
	 * @param rapportAModifier
	 * @param request
	 * @param nouveauxRapports
	 */
	private void handleRapportSurPeriode(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request, List<RapportPrestationImposable> nouveauxRapports) {
		//CAS 2, 3

		final RegDate dateFin = rapportAModifier.getDateFin();
		final RegDate dateDebutPeriodeDeclaration = request.getDateDebutPeriodeDeclaration();
		final RegDate dateFinPeriodeDeclaration = request.getDateFinPeriodeDeclaration();

		if (dateFin == null) {
			if (isEvenementFinRapportTravail(request)) {
				final RegDate dateFermeture = calculerDateFinRapportTravail(request);
				fermerRapportTravail(rapportAModifier, dateFermeture);
			}
			else {
				final String cause = "Rapport sans date de fin, aucun evenement de fermeture ou de fin de rapport dans la demande";
				aucunTraitement(cause, rapportAModifier, request);
			}

		}
		else {
			if (dateFin.isAfter(dateFinPeriodeDeclaration)) {
				final String cause = "le Rapport a une date de fin, postérieur à la date de fin de la période de déclaration:";
				aucunTraitement(cause, rapportAModifier, request);

			}
			else if (dateFin.isAfterOrEqual(dateDebutPeriodeDeclaration) && dateFin.isBefore(dateFinPeriodeDeclaration)) {
				if (isEvenementFinRapportTravail(request)) {
					final String cause = "le Rapport a déjà une date de fin au %S :";
					aucunTraitement(cause, rapportAModifier, request);

				}
				else {
					reouvrirRapportTravail(rapportAModifier, request, nouveauxRapports);
				}
			}
		}

	}

	private void reouvrirRapportTravail(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request, List<RapportPrestationImposable> nouveauxRapports) {


		loggerModification("Reouverture ", rapportAModifier);
		final RapportPrestationImposable rapportPrestationImposable = new RapportPrestationImposable(rapportAModifier);
		rapportPrestationImposable.setDateFin(null);
		rapportAModifier.setAnnule(true);
		nouveauxRapports.add(rapportPrestationImposable);


	}

	/**
	 * Gere le cas ou on a une evenement de fermeture avec la presence d'un rapport de travail
	 *
	 * @param rapportAModifier rapport de travail trouvé pour la periode
	 * @param request          la demande de modification
	 */
	private void handleEvenementFermetureRapportTravailExistant(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {
		final RegDate dateDebutRapportTravail = rapportAModifier.getDateDebut();
		final RegDate dateDebutPeriodeDeclaration = request.getDateDebutPeriodeDeclaration();

		if (dateDebutRapportTravail.isAfterOrEqual(dateDebutPeriodeDeclaration)) {
			//CAS 18
			annulerRapportTravail(rapportAModifier);
		}
		else if (dateDebutRapportTravail.isBefore(dateDebutPeriodeDeclaration)) {
			//Traitement de la date de fin du rappport
			traiterFermetureRapportTravail(rapportAModifier, request);
		}
	}

	/**
	 * Applique les règles de fermeture du rapport de travail en cas d'évenement de fermeture (Z)
	 *
	 * @param rapportAModifier rapport de travail trouvé pour la periode
	 * @param request          la demande de mis a jour
	 */
	private void traiterFermetureRapportTravail(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {

		final RegDate dateFermeture = request.getDateDebutPeriodeDeclaration().getOneDayBefore();
		final RegDate dateFinDeclaration = request.getDateFinPeriodeDeclaration();

		//cas 16 et cas 15
		final RegDate dateFinRapport = rapportAModifier.getDateFin();
		if (dateFinRapport == null || dateFinRapport.isBeforeOrEqual(dateFinDeclaration)) {
			fermerRapportTravail(rapportAModifier, dateFermeture);
		}
		//cas 17
		if (dateFinRapport != null && dateFinRapport.isBefore(request.getDateDebutPeriodeDeclaration())) {
			final String cause = "Date de fin du rapport avant la date de début de la période";
			aucunTraitement(cause, rapportAModifier, request);
		}

	}

	/**
	 * Ferme un rapport de travail
	 *
	 * @param rapportAModifier rapport de travail à fermer
	 * @param dateFermeture    la date de fin du rapport
	 */
	private void fermerRapportTravail(RapportPrestationImposable rapportAModifier, RegDate dateFermeture) {
		rapportAModifier.setDateFin(dateFermeture);
		String message = String.format("Fermeture du rapport de travail commencant" +
				" le %s pour le debiteur %s et le sourcier %s. La date de fermeture a été determinée au %s",
				RegDateHelper.dateToDisplayString(rapportAModifier.getDateDebut()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getObjetId()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getSujetId()),
				RegDateHelper.dateToDisplayString(dateFermeture));
		LOGGER.info(message);
	}

	/**
	 * Effectue l'annulation du rapport de travail
	 *
	 * @param rapportAModifier rapport de travail trouvé pour la periode
	 */
	private void annulerRapportTravail(RapportPrestationImposable rapportAModifier) {
		rapportAModifier.setAnnule(true);
		String message = String.format("Annulation du rapport de travail commencant" +
				" le %s pour le debiteur %s et le sourcier %s",
				RegDateHelper.dateToDisplayString(rapportAModifier.getDateDebut()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getObjetId()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getSujetId())
		);
		LOGGER.info(message);
	}

	private MiseAJourRapportTravailResponse createResponse(MiseAjourRapportTravail request) {
		MiseAJourRapportTravailResponse response = new MiseAJourRapportTravailResponse();
		response.setDatePriseEnCompte(DataHelper.coreToXML(RegDate.get()));
		return response;
	}

	/**
	 * Permet de traiter le cas ou le rapport de travail est inexistant
	 *
	 * @param dpi              le débiteur
	 * @param sourcier         le sourcier
	 * @param request          la demande de mise à jour
	 * @param nouveauxRapports
	 */
	private void handleRapportPrestationInexistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, MiseAjourRapportTravail request, List<RapportPrestationImposable> nouveauxRapports) {


		//On a pas d'évènement de fermeture de rapport de travail
		if (!request.isFermetureRapportTravail()) {
			creerRapportTravail(dpi, sourcier, request, nouveauxRapports);
		}
		else {
			//Si le rapport n'existe pas et qu 'on reçoit un evenement de fermeture, on ignore le message
			ignorerMessagePourRTInexistant(dpi, sourcier, request.getBusinessId());
		}
	}


	/**
	 * Trouve le rapport de Travail qui est concerné par la période de déclaration
	 *
	 * @param dpi      un débiteur
	 * @param sourcier le sourcier
	 * @return le premier rapport de travail qui est concerne par la période de déclaration
	 */
	private List<RapportPrestationImposable> findRapportPrestationImposable(DebiteurPrestationImposable dpi, PersonnePhysique sourcier) {
		List<RapportPrestationImposable> listeRapport = tiersService.getAllRapportPrestationImposable(dpi, sourcier, true, true);
		return listeRapport;
	}


	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/rt/rapport-travail-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Arrays.asList(new ClassPathResource("event/rt/rapport-travail-response-1.xsd"));
	}

	private void validateDebiteur(DebiteurPrestationImposable dpi, DateRange periodeDeclaration) throws ServiceException {

		final List<ForFiscal> fors = dpi.getForsFiscauxNonAnnules(true);
		final List<DateRange> forRanges = new ArrayList<DateRange>(fors);

		final List<DateRange> periodeNonCouverte = DateRangeHelper.subtract(periodeDeclaration, forRanges);
		if (!periodeNonCouverte.isEmpty()) {
			final String msg = String.format("le débiteur (%s) ne possède pas de fors couvrant la totalité de la période de déclaration qui va du %s au %s.",
					FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()), RegDateHelper.dateToDisplayString(periodeDeclaration.getDateDebut()),
					RegDateHelper.dateToDisplayString(periodeDeclaration.getDateFin()));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.VALIDATION.name(), null));
		}

	}


	private void creerRapportTravail(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, MiseAjourRapportTravail request, List<RapportPrestationImposable> nouveauxRapports) {
		RegDate dateDebut = request.getDateDebutVersementSalaire();
		RegDate dateFin = calculerDateFinRapportTravail(request);
		//Afin d'éviter les doublons annulés inutiles, on teste la presence eventuelle du rapport que l'on se prépare a créér.
		//SIFISC-7541
		if (isRapportPresent(dateDebut, dateFin, dpi, sourcier)) {
			aucunTraitement("Rapport à créer déjà existant ",dateDebut,dateFin,request);

		}
		else{
			final RapportPrestationImposable rapportPrestationImposable = new RapportPrestationImposable(dateDebut, dateFin, sourcier, dpi);
			nouveauxRapports.add(rapportPrestationImposable);

			final String message = String.format("Création d'un nouveau rapport de travail entre le débiteur %s et le sourcier %s." +
					"  Date début du rapport: %s, date de fin: %s", FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()),
					FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()),
					RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin));
			LOGGER.info(message);
		}


	}

	private boolean isRapportPresent(RegDate dateDebut, RegDate dateFin, DebiteurPrestationImposable dpi, PersonnePhysique sourcier) {
		final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi,sourcier,true,true);
		for (RapportPrestationImposable rapport : rapports) {
			if (rapport.getDateDebut() == dateDebut && rapport.getDateFin() == dateFin) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Permet de calculer la date de fin d'un rapport de travail en fonction de la présence d'un évènement de fin de rapport: si l'évenement est une sortie alors la date fin prend la valeur de la date de
	 * fin de versement de salaire si l'évenement est un décès alors la date de fin correspond à la date de l'évènement.
	 *
	 * @param request demande de mise à jour du rapport de travail
	 * @return la bonne date de fin en fonction de l'évenement reçu
	 */
	private RegDate calculerDateFinRapportTravail(MiseAjourRapportTravail request) {
		//Est ce que c'est une fin de rapport de travail
		if (request.isSortie()) {
			return request.getDateFinVersementSalaire();
		}

		if (request.isDeces()) {
			return request.getDateEvenement();
		}
		return null;
	}

	private boolean isEvenementFinRapportTravail(MiseAjourRapportTravail request) {
		return request.isDeces() || request.isSortie();
	}

	private void ignorerMessagePourRTInexistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, String businessId) {
		final String message = String.format("le message ayant comme business id %s et qui concerne le dbiteur %s et le sourcier %s." +
				"  sera ignoré car aucun rapport de travail n'a été trouvé pour cette demande de fermeture", businessId, FormatNumeroHelper.
				numeroCTBToDisplay(dpi.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()));
		LOGGER.info(message);
	}

	private void aucunTraitement(String cause, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {
		aucunTraitement(cause, rapportAModifier.getDateDebut(),rapportAModifier.getDateFin(), request);

	}
	private void aucunTraitement(String cause,RegDate dateDebut, RegDate dateFin, MiseAjourRapportTravail request) {
		String message = String.format("Aucun traitement necessaire pour le message %s concernant le rapport de travail commencant" +
				" le %s, se terminant le %s pour le debiteur %s et le sourcier %s ",
				request.getBusinessId(),
				RegDateHelper.dateToDisplayString(dateDebut),
				RegDateHelper.dateToDisplayString(dateFin),
				FormatNumeroHelper.numeroCTBToDisplay(request.getIdDebiteur()),
				FormatNumeroHelper.numeroCTBToDisplay(request.getIdSourcier()));
		LOGGER.info(message + ": " + cause);
	}

	private boolean isRapportFermeAvantPeriodeDeclaration(RapportPrestationImposable rapport, MiseAjourRapportTravail request) {
		final RegDate dateFin = rapport.getDateFin();
		return dateFin != null && dateFin.isBefore(request.getDateDebutPeriodeDeclaration());
	}

	private boolean isRapportOuvertApresPeriode(RapportPrestationImposable rapport, MiseAjourRapportTravail request) {
		final RegDate dateDebut = rapport.getDateDebut();
		return dateDebut != null && dateDebut.isAfter(request.getDateFinPeriodeDeclaration());
	}

	private boolean isRapportSurPeriode(RapportPrestationImposable rapport, MiseAjourRapportTravail request) {
		final DateRangeHelper.Range periodeDeclaration = new DateRangeHelper.Range(request.getDateDebutPeriodeDeclaration(), request.getDateFinPeriodeDeclaration());
		return DateRangeHelper.intersect(rapport, periodeDeclaration);
	}
}