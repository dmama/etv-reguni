package ch.vd.unireg.evenement.rapport.travail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeComparator;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.validation.ValidationException;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.unireg.common.CollectionsUtils;
import ch.vd.unireg.common.FormatNumeroHelper;
import ch.vd.unireg.hibernate.HibernateTemplate;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.RapportEntreTiers;
import ch.vd.unireg.tiers.RapportPrestationImposable;
import ch.vd.unireg.tiers.Tiers;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.xml.Context;
import ch.vd.unireg.xml.DataHelper;
import ch.vd.unireg.xml.ServiceException;


public class MiseAJourRapportTravailRequestHandler implements RapportTravailRequestHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(MiseAJourRapportTravailRequestHandler.class);

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
		annulerRapportMultiple(dpi, sourcier);
		final List<RapportPrestationImposable> rapportsAModifier = findRapportsPrestationImposableNonAnnules(dpi, sourcier);
		final List<RapportPrestationImposable> nouveauxRapports = new ArrayList<>();

		if (rapportsAModifier == null || rapportsAModifier.isEmpty()) {
			handleRapportPrestationInexistant(dpi, sourcier, request, nouveauxRapports);
		}
		else {
			// premier cleanup pour éliminer les chevauchements pré-existants
			cleanupChevauchements(dpi, sourcier, rapportsAModifier, nouveauxRapports);

			final ArrayList<RapportPrestationImposable> nouveauxJusquici = new ArrayList<>(nouveauxRapports);
			for (RapportPrestationImposable rapportAModifier : CollectionsUtils.merged(rapportsAModifier, nouveauxJusquici)) {
				// attention ! un rapport pré-existant a pu être annulé dans le cleanup des chevauchements...
				if (!rapportAModifier.isAnnule()) {
					handleRapportPrestationExistant(dpi, sourcier, rapportAModifier, request, nouveauxRapports);
				}
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

	private static void cleanupChevauchements(DebiteurPrestationImposable dpi, PersonnePhysique sourcier,
	                                          List<RapportPrestationImposable> anciensRapports,
	                                          List<RapportPrestationImposable> nouveauRapports) {

		// il faut d'abord constituer des groupes
		// -> si on trie les anciens rapports selon leur date de début, et qu'on les prend en compte dans cet ordre, on peut être certain
		// qu'on ne devra pas fusionner des groupes (si un élément n'intersecte pas un groupe déjà formé, l'élément suivant ne pourra pas non-plus
		// intersecter l'un de ces groupes)
		final List<RapportPrestationImposable> listeTriee = new ArrayList<>(anciensRapports);
		listeTriee.sort(new DateRangeComparator<>());

		// constitution des groupes
		final List<List<RapportPrestationImposable>> groupes = new ArrayList<>(anciensRapports.size());
		List<RapportPrestationImposable> groupeCourant = null;
		for (RapportPrestationImposable elt : listeTriee) {
			if (groupeCourant == null || !DateRangeHelper.intersect(elt, groupeCourant)) {
				groupeCourant = new ArrayList<>(anciensRapports.size());
				groupes.add(groupeCourant);
			}
			groupeCourant.add(elt);
		}

		// si autant de groupes que de rapports initiaux, il n'y a pas de chevauchement, on peut s'arrêter là
		if (groupes.size() != anciensRapports.size()) {
			for (List<RapportPrestationImposable> groupe : groupes) {
				// on ne s'intéresse bien-sûr qu'aux groupes qui contiennent plusieurs éléments...
				if (groupe.size() > 1) {
					// si on trouve un élément qui englobe tous les autres, on le prend et on élimine tous les autres
					// sinon, on élimine tout le monde et on crée un nouveau qui englobe tout
					boolean first = true;
					RegDate min = null;
					RegDate max = null;
					for (RapportPrestationImposable elt : groupe) {
						if (first) {
							min = elt.getDateDebut();
							max = elt.getDateFin();
							first = false;
						}
						else {
							min = RegDateHelper.minimum(min, elt.getDateDebut(), NullDateBehavior.EARLIEST);
							max = RegDateHelper.maximum(max, elt.getDateFin(), NullDateBehavior.LATEST);
						}
					}

					// nouveau tour pour savoir si un élément englobe le tout
					RapportPrestationImposable master = null;
					for (RapportPrestationImposable elt : groupe) {
						if (elt.getDateDebut() == min && elt.getDateFin() == max) {
							// trouvé
							master = elt;
							break;
						}
					}

					// on annule tous les rapports qui ne sont pas l'englobant
					for (RapportPrestationImposable elt : groupe) {
						if (elt != master) {
							elt.setAnnule(true);
							LOGGER.info(String.format("Nettoyage des chevauchements initiaux : annulation du rapport sur la période %s",
							                          DateRangeHelper.toDisplayString(elt)));
						}
					}

					// si on n'a pas d'englobant, il faut en créer un
					if (master == null) {
						LOGGER.info(String.format("Nettoyage des chevauchements initiaux : création d'un rapport consolidé sur la période %s", DateRangeHelper.toDisplayString(min, max)));
						nouveauRapports.add(new RapportPrestationImposable(min, max, sourcier, dpi));
					}
				}
			}
		}
	}

	private void addNewRapport(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, List<RapportPrestationImposable> nouveauxRapports) {
		for (RapportPrestationImposable nouveauRapport : nouveauxRapports) {
			// on ne rajoute pas les nouveaux rapports qui ont été annulés depuis...
			if (!nouveauRapport.isAnnule()) {
				tiersService.addRapportPrestationImposable(sourcier, dpi, nouveauRapport.getDateDebut(), nouveauRapport.getDateFin());
			}
			else {
				LOGGER.info(String.format("Le rapport précédemment créé sur la période %s n'a finalement pas été sauvegardé en base car il a été annulé par la suite.",
				                          DateRangeHelper.toDisplayString(nouveauRapport)));
			}
		}
	}

	private static List<RapportPrestationImposable> getRapportPrestation(DebiteurPrestationImposable dpi, PersonnePhysique sourcier) {
		final List<RapportPrestationImposable> rapports = new ArrayList<>();
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
					fermerRapportTravail(rapportPrestationImposable, request.getDateFinPeriodeDeclaration(), "date de fermeture du dernier for débiteur");
				}
			}
			//on ferme tous les rapports encore ouverts a cette date la dans la liste des nouveaux rapports
			for (RapportPrestationImposable rapportPrestationImposable : nouveauxRapports) {

				if (rapportPrestationImposable.getDateFin() == null) {
					//la date de fin du for est la même que la de fin de période
					fermerRapportTravail(rapportPrestationImposable, request.getDateFinPeriodeDeclaration(), "date de fermeture du dernier for débiteur");
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
		final List<RapportPrestationImposable> rapportsConcernes = new ArrayList<>();
		for (RapportPrestationImposable rapport : rapportsAModifier) {
			if (DateRangeHelper.intersect(periodeDeclaration, rapport) && !rapport.isAnnule()) {
				rapportsConcernes.add(rapport);
			}
		}

		for (RapportPrestationImposable rapport : nouveauxRapports) {
			if (DateRangeHelper.intersect(periodeDeclaration, rapport) && !rapport.isAnnule()) {
				rapportsConcernes.add(rapport);
			}
		}

		if (rapportsConcernes.size() > 1) {

			//on orddonnes les rapports;
			rapportsConcernes.sort(new DateRangeComparator<>());

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
					final String message = String.format("Traitement des doublons et des chevauchements: Annulation du rapport commençant le '%s'" +
							" et se terminant le '%s' pour le débiteur %s et le sourcier %s. Il sera remplacé par un nouveau rapport de prestation avec les dates de début et de fin adaptées",
							RegDateHelper.dateToDisplayString(rapportsConcerne.getDateDebut()),
							RegDateHelper.dateToDisplayString(rapportsConcerne.getDateFin()),
							FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()),
							FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()));
					LOGGER.info(message);
				}
				else {
					//on peut supprimer ce nouveau rapport de la liste des rapport à ajouter
					nouveauxRapports.remove(rapportsConcerne);
					final String message = String.format("Traitement des doublons et des chevauchements: Suppression du rapport temporaire calculé commençant le '%s'" +
							" et se terminant le '%s' pour le débiteur %s et le sourcier %s. Il sera remplacé par un nouveau rapport de prestation avec les dates de début et de fin adaptées.",
							RegDateHelper.dateToDisplayString(rapportsConcerne.getDateDebut()),
							RegDateHelper.dateToDisplayString(rapportsConcerne.getDateFin()),
							FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()),
							FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()));
					LOGGER.info(message);
				}


			}

			String message = String.format("Nouveau rapport de travail créé suite à la détection de chevauchement: " +
					"Ce nouveau rapport commence le '%s' et se termine le '%s'.  Concerne le debiteur %s et le sourcier %s.",
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
			final String msg = String.format("Le sourcier %s n'existe pas dans unireg", FormatNumeroHelper.numeroCTBToDisplay(numeroSourcier));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}

		if (!(tiers instanceof PersonnePhysique)) {
			final String msg = String.format("Le sourcier %s ne correspond pas à une personne physique", FormatNumeroHelper.numeroCTBToDisplay(numeroSourcier));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.INVALID_PARTY_TYPE.name(), null));
		}

		return (PersonnePhysique) tiers;
	}

	private DebiteurPrestationImposable getDebiteur(MiseAjourRapportTravail request) throws ServiceException {

		final long numeroDpi = request.getIdDebiteur();
		final Tiers tiers = tiersService.getTiers(numeroDpi);
		//le débiteur doit exister
		if (tiers == null) {
			final String msg = String.format("Le débiteur %s n'existe pas dans unireg", FormatNumeroHelper.numeroCTBToDisplay(numeroDpi));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}

		//nature du tiers récupéré
		if (!(tiers instanceof DebiteurPrestationImposable)) {
			final String msg = String.format("Le tiers %s ne correspond pas à un débiteur", FormatNumeroHelper.numeroCTBToDisplay(numeroDpi));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.INVALID_PARTY_TYPE.name(), null));
		}

		return (DebiteurPrestationImposable) tiers;
	}

	private void handleRapportPrestationExistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request,
	                                             List<RapportPrestationImposable> nouveauxRapports) {
		//On a un evenement de fermeture de rapport de travail
		if (request.isFermetureRapportTravail()) {
			handleEvenementFermetureRapportTravailExistant(rapportAModifier, request, nouveauxRapports);
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
		// [SIFISC-8945] l'avant veille (même au soir, puisqu'il s'agit d'une date de fin...) a un décalage de plus d'un jour avec le (matin du) jour courant
		// -> s'il y a un jour (= 24 heures) d'absence de RT, on ne ré-ouvre pas le RT
		final RegDate veille = dateDebutPeriode == null ? null : dateDebutPeriode.getOneDayBefore();
		return dateFin == veille;
	}

	private void handleRapportOuvertApresPeriode(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request, List<RapportPrestationImposable> nouveauxRapports) {

		final RegDate dateDebutVersementSalaire = request.getDateDebutVersementSalaire();
		final String modification = String.format("Modification de la date de début, nouvelle date: %s ", RegDateHelper.dateToDisplayString(dateDebutVersementSalaire));
		loggerModification(modification, rapportAModifier);

		final RapportPrestationImposable rapportPrestationImposable = new RapportPrestationImposable(rapportAModifier);
		rapportPrestationImposable.setDateDebut(dateDebutVersementSalaire);
		rapportAModifier.setAnnule(true);
		nouveauxRapports.add(rapportPrestationImposable);
	}

	private void loggerModification(String modification, RapportPrestationImposable rapportAModifier) {
		String message = String.format("%s pour le rapport de travail commençant le '%s' pour le debiteur %s et le sourcier %s.",
		                               modification,
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
				fermerRapportTravail(rapportAModifier, dateFermeture, "événement de sortie/décès");
			}
			else {
				final String cause = "Rapport sans date de fin, aucun événement de fermeture ou de fin de rapport dans la demande";
				aucunTraitement(cause, rapportAModifier, request);
			}
		}
		else {
			if (dateFin.isAfterOrEqual(dateFinPeriodeDeclaration)) {
				final String cause = "Le rapport a une date de fin, postérieure ou égale à la date de fin de la période de déclaration";
				aucunTraitement(cause, rapportAModifier, request);
			}
			else if (dateFin.isAfterOrEqual(dateDebutPeriodeDeclaration)) {
				if (isEvenementFinRapportTravail(request)) {
					final String cause = "Le rapport a déjà une date de fin postérieure à la date de début de la période de déclaraton";
					aucunTraitement(cause, rapportAModifier, request);
				}
				else {
					reouvrirRapportTravail(rapportAModifier, request, nouveauxRapports);
				}
			}
			else  {
				throw new IllegalStateException("Que fait-on ici ? Le cas où la date de fin du rapport existant est antérieure à la date de début de la période de déclaration devrait être géré ailleurs...");
			}
		}
	}

	private void reouvrirRapportTravail(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request, List<RapportPrestationImposable> nouveauxRapports) {
		loggerModification("Ré-ouverture", rapportAModifier);
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
	 * @param nouveauxRapports
	 */
	private void handleEvenementFermetureRapportTravailExistant(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request, List<RapportPrestationImposable> nouveauxRapports) {
		final RegDate dateDebutRapportTravail = rapportAModifier.getDateDebut();
		final RegDate dateDebutPeriodeDeclaration = request.getDateDebutPeriodeDeclaration();

		if (dateDebutRapportTravail.isAfterOrEqual(dateDebutPeriodeDeclaration)) {
			//CAS 18
			annulerRapportTravail(rapportAModifier);
		}
		else {
			//Traitement de la date de fin du rappport
			traiterFermetureRapportTravail(rapportAModifier, request, nouveauxRapports);
		}
	}

	/**
	 * Applique les règles de fermeture du rapport de travail en cas d'évenement de fermeture (Z)
	 *
	 * @param rapportAModifier rapport de travail trouvé pour la periode
	 * @param request          la demande de mis a jour
	 * @param nouveauxRapports
	 */
	private void traiterFermetureRapportTravail(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request, List<RapportPrestationImposable> nouveauxRapports) {

		final RegDate dateFermeture = request.getDateDebutPeriodeDeclaration().getOneDayBefore();
		final RegDate dateFinDeclaration = request.getDateFinPeriodeDeclaration();

		// cas 17
		final RegDate dateFinRapport = rapportAModifier.getDateFin();
		if (dateFinRapport != null && dateFinRapport.isBefore(request.getDateDebutPeriodeDeclaration())) {
			final String cause = "Date de fin du rapport avant la date de début de la période";
			aucunTraitement(cause, rapportAModifier, request);
		}
		// cas 15
		else if (dateFinRapport == null) {
			fermerRapportTravail(rapportAModifier, dateFermeture, "événement de fermeture");
		}
		// cas 16
		else if (dateFinRapport.isBeforeOrEqual(dateFinDeclaration)) {
			modifierFinRapportTravail(rapportAModifier, dateFermeture, nouveauxRapports);
		}
	}

	/**
	 * Ferme un rapport de travail
	 *
	 * @param rapportAModifier rapport de travail à fermer
	 * @param dateFermeture    la date de fin du rapport
	 */
	private void fermerRapportTravail(RapportPrestationImposable rapportAModifier, RegDate dateFermeture, String motifPourLog) {
		rapportAModifier.setDateFin(dateFermeture);
		final String message = String.format("Fermeture du rapport de travail commençant le %s pour le debiteur %s et le sourcier %s. La date de fermeture a été determinée au %s (%s).",
				RegDateHelper.dateToDisplayString(rapportAModifier.getDateDebut()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getObjetId()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getSujetId()),
				RegDateHelper.dateToDisplayString(dateFermeture),
				motifPourLog);
		LOGGER.info(message);
	}

	/**
	 * Modifie la date de fin d'un rapport de travail en passant par l'annulation du rapport existant et la création d'un autre
	 */
	private void modifierFinRapportTravail(RapportPrestationImposable rapportAModifier, RegDate dateFermeture, List<RapportPrestationImposable> nouveauxRapports) {
		final RapportPrestationImposable rpi = new RapportPrestationImposable(rapportAModifier);
		rpi.setDateFin(dateFermeture);
		rapportAModifier.setAnnule(true);
		nouveauxRapports.add(rpi);

		final String modification = String.format("Modification de la date de fin, nouvelle date: %s ", RegDateHelper.dateToDisplayString(dateFermeture));
		loggerModification(modification, rapportAModifier);
	}

	/**
	 * Effectue l'annulation du rapport de travail
	 *
	 * @param rapportAModifier rapport de travail trouvé pour la periode
	 */
	private void annulerRapportTravail(RapportPrestationImposable rapportAModifier) {
		rapportAModifier.setAnnule(true);
		String message = String.format("Annulation du rapport de travail commençant le %s pour le debiteur %s et le sourcier %s",
				RegDateHelper.dateToDisplayString(rapportAModifier.getDateDebut()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getObjetId()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getSujetId())
		);
		LOGGER.info(message);
	}

	private MiseAJourRapportTravailResponse createResponse(MiseAjourRapportTravail request) {
		MiseAJourRapportTravailResponse response = new MiseAJourRapportTravailResponse();
		response.setDatePriseEnCompte(DataHelper.coreToXMLv1(RegDate.get()));
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
	 * Trouve les rapports de travail entre le débiteur et le sourcier considérés
	 * @param dpi      un débiteur
	 * @param sourcier le sourcier
	 * @return tous les rapports de travail non-annulés entre ces deux-là
	 */
	private List<RapportPrestationImposable> findRapportsPrestationImposableNonAnnules(DebiteurPrestationImposable dpi, PersonnePhysique sourcier) {
		return tiersService.getAllRapportPrestationImposable(dpi, sourcier, true, true);
	}

	@Override
	public ClassPathResource getRequestXSD() {
		return new ClassPathResource("event/rt/rapport-travail-request-1.xsd");
	}

	@Override
	public List<ClassPathResource> getResponseXSD() {
		return Collections.singletonList(new ClassPathResource("event/rt/rapport-travail-response-1.xsd"));
	}

	private void validateDebiteur(DebiteurPrestationImposable dpi, DateRange periodeDeclaration) throws ServiceException {

		final List<ForFiscal> fors = dpi.getForsFiscauxNonAnnules(true);
		final List<DateRange> forRanges = new ArrayList<>(fors);

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
			aucunTraitement("Rapport à créer déjà existant", dateDebut, dateFin, request);
		}
		else if (!DateRangeHelper.isFullyCovered(new DateRangeHelper.Range(dateDebut, dateFin), nouveauxRapports)) {
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
		final List<RapportPrestationImposable> rapports = tiersService.getAllRapportPrestationImposable(dpi, sourcier, true, true);
		final DateRange range = new DateRangeHelper.Range(dateDebut, dateFin);
		return DateRangeHelper.isFullyCovered(range, rapports);
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
		final String message = String.format("Le message ayant comme business id '%s' et qui concerne le débiteur %s et le sourcier %s sera ignoré car aucun rapport de travail n'a été trouvé pour cette demande de fermeture", businessId, FormatNumeroHelper.
				numeroCTBToDisplay(dpi.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()));
		LOGGER.info(message);
	}

	private void aucunTraitement(String cause, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {
		aucunTraitement(cause, rapportAModifier.getDateDebut(), rapportAModifier.getDateFin(), request);
	}

	private void aucunTraitement(String cause, RegDate dateDebut, RegDate dateFin, MiseAjourRapportTravail request) {
		final String message = String.format("Aucun traitement necessaire pour le message %s concernant le rapport de travail commençant le %s, se terminant le %s pour le debiteur %s et le sourcier %s : %s",
				request.getBusinessId(), RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin),
				FormatNumeroHelper.numeroCTBToDisplay(request.getIdDebiteur()), FormatNumeroHelper.numeroCTBToDisplay(request.getIdSourcier()), cause);
		LOGGER.info(message);
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

	/**
	 * Annule les rapports de travail multiples ouvert qu'il nous faut enlever du traitement
	 *
	 * @param dpi le debiteur
	 * @param sourcier Le sourcier lié
	 */
	private void annulerRapportMultiple(DebiteurPrestationImposable dpi, PersonnePhysique sourcier) {

		final List<RapportPrestationImposable> rapportsAModifier = findRapportsPrestationImposableNonAnnules(dpi, sourcier);
		final List<RapportPrestationImposable> rapportMultiples = new ArrayList<>();
		final List<RapportPrestationImposable> rapportsOuverts = new ArrayList<>();
		for (RapportPrestationImposable rapportAModifier : rapportsAModifier) {
			if (rapportAModifier.getDateFin() == null) {
				rapportsOuverts.add(rapportAModifier);
			}
		}
		if (!rapportsOuverts.isEmpty() && rapportsOuverts.size()>1) {
			//Rapport qui devra être traiter donc ne devra pas être annulé
			RapportPrestationImposable rapportAConserver = null;

				for (RapportPrestationImposable rapportOuvert : rapportsOuverts) {
					if (rapportAConserver == null || rapportAConserver.getDateDebut().isAfter(rapportOuvert.getDateDebut())) {

						if (rapportAConserver != null) {
							//Le rapport courant est à mettre dans les rapports multiples à annuler
							rapportMultiples.add(rapportAConserver);
						}
						rapportAConserver = rapportOuvert;

					}
					else{
						rapportMultiples.add(rapportOuvert);
					}
				}

		}

		for (RapportPrestationImposable rapportMultiple : rapportMultiples) {
			rapportMultiple.setAnnule(true);
		}
	}
}