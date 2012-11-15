package ch.vd.uniregctb.evenement.rapport.travail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.xml.event.rt.response.v1.MiseAJourRapportTravailResponse;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionInfo;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportPrestationImposable;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.TypeActivite;
import ch.vd.uniregctb.xml.DataHelper;
import ch.vd.uniregctb.xml.ServiceException;


public class MiseAJourRapportTravailRequestHandler implements RapportTravailRequestHandler {

	private static final Logger LOGGER = Logger.getLogger(MiseAJourRapportTravailRequestHandler.class);

	private TiersService tiersService;


	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}


	@Override
	public MiseAJourRapportTravailResponse handle(MiseAjourRapportTravail request) throws ServiceException {


		//Initialisation de la période de déclaration
		final DateRange periodeDeclaration = new DateRangeHelper.Range(request.getDateDebutPeriodeDeclaration(), request.getDateFinPeriodeDeclaration());
		final DebiteurPrestationImposable dpi = getDebiteur(request);

		// Le débiteur doit être actif (For en vigueur) sur toute la période de déclaration
		validateDebiteur(dpi, periodeDeclaration);
		final PersonnePhysique sourcier = getSourcier(request);


		//On retrouve le rapport a modifier.
		final List<RapportPrestationImposable> rapportsAModifier = findRapportPrestationImposable(dpi, sourcier);

		if (rapportsAModifier == null || rapportsAModifier.isEmpty()) {
			handleRapportPrestationInexistant(dpi, sourcier, request);
		}
		else {
			for (RapportPrestationImposable rapportAModifier : rapportsAModifier) {
				handleRapportPrestationExistant(dpi, sourcier, rapportAModifier, request);
			}

		}
		return createResponse(request);
	}




	private PersonnePhysique getSourcier(MiseAjourRapportTravail request) throws ServiceException {
		//Le sourcier doit exister
		final long numeroSourcier = request.getIdSourcier();
		final PersonnePhysique sourcier = (PersonnePhysique) tiersService.getTiers(numeroSourcier);
		if (sourcier == null) {
			final String msg = String.format("le sourcier %s n'existe pas dans unireg", FormatNumeroHelper.numeroCTBToDisplay(numeroSourcier));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}
		return sourcier;
	}

	private DebiteurPrestationImposable getDebiteur(MiseAjourRapportTravail request) throws ServiceException {
		//le débiteur doit exister
		final long numeroDpi = request.getIdDebiteur();
		final DebiteurPrestationImposable dpi = (DebiteurPrestationImposable) tiersService.getTiers(numeroDpi);
		if (dpi == null) {
			final String msg = String.format("le débiteur %s n'existe pas dans unireg", FormatNumeroHelper.numeroCTBToDisplay(numeroDpi));
			throw new ServiceException(new BusinessExceptionInfo(msg, BusinessExceptionCode.UNKNOWN_PARTY.name(), null));
		}
		return dpi;
	}

	private void handleRapportPrestationExistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {
		//On a un evenement de fermeture de rapport de travail
		if (request.isFermetureRapportTravail()) {
			handleEvenementFermetureRapportTravailExistant(rapportAModifier, request);

		} else{
			handleModificationRapportTravailExistant(dpi, sourcier, rapportAModifier, request);
		}
	}

	private void handleModificationRapportTravailExistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {

		if(isRapportSurPeriode(rapportAModifier, request)){
			handleRapportSurPeriode(rapportAModifier, request);
		}
		else if(isRapportOuvertApresPeriode(rapportAModifier, request)){
			handleRapportOuvertApresPeriode(rapportAModifier, request);
		}
		else if(isRapportFermeAvantPeriodeDeclaration(rapportAModifier,request)){
			handleRapportFermeAvantPeriodeDeclaration(dpi, sourcier, rapportAModifier,request);
		}

	}

	private void handleRapportFermeAvantPeriodeDeclaration(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {
		final RegDate dateFin = rapportAModifier.getDateFin();
		final RegDate dateDebutPeriode = request.getDateDebutPeriodeDeclaration();
		if(isEcartInferieurEgalAUnJour(dateDebutPeriode, dateFin)){

			reouvrirRapportTravail(rapportAModifier,request);
			final RegDate nouvelleDateFin = calculerDateFinRapportTravail(request);
			if(nouvelleDateFin!=null){
				fermerRapportTravail(rapportAModifier,nouvelleDateFin);
			}
		}
		else{
			creerRapportTravail(dpi,sourcier,request);

		}
	}

	private boolean isEcartInferieurEgalAUnJour(RegDate dateDebutPeriode, RegDate dateFin) {
		final RegDate veille = dateDebutPeriode.getOneDayBefore();
		final RegDate avantVeille = veille.getOneDayBefore();
		if(dateFin.equals(veille) || dateFin.equals(avantVeille)){
			return true;
		}
		return false;
	}

	private void handleRapportOuvertApresPeriode(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {

		final RegDate dateDebutVersementSalaire = request.getDateDebutVersementSalaire();

		final String modification = String.format("Modification de la date de début, nouvelle date: %s ", dateDebutVersementSalaire);

		loggerModification(modification,rapportAModifier);

		rapportAModifier.setDateDebut(dateDebutVersementSalaire);



	}

	private void loggerModification(String modification,RapportPrestationImposable rapportAModifier) {
		String message = String.format(modification+ " pour le rapport de travail commencant" +
				" le %s pour le debiteur %s et le sourcier %s.",
				RegDateHelper.dateToDisplayString(rapportAModifier.getDateDebut()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getObjetId()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getSujetId()));
		LOGGER.info(message);
	}

	/**Applique les règles de modification de date de fin sur un rapport qui recoupe la periode de declaration du message
	 *
	 * @param rapportAModifier
	 * @param request
	 */
	private void handleRapportSurPeriode(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {
		//CAS 2, 3

		final RegDate dateFin = rapportAModifier.getDateFin();
		final RegDate dateDebutPeriodeDeclaration = request.getDateDebutPeriodeDeclaration();
		final RegDate dateFinPeriodeDeclaration = request.getDateFinPeriodeDeclaration();

		if(dateFin ==null){
			if(isEvenementFinRapportTravail(request)){
				final RegDate dateFermeture = calculerDateFinRapportTravail(request);
				fermerRapportTravail(rapportAModifier,dateFermeture);
			}else{
				final String cause = "Rapport sans date de fin, aucun evenement de fermeture ou de fin de rapport dans la demande";
				aucunTraitement(cause, rapportAModifier, request);
			}

		}else {
			if(dateFin.isAfter(dateFinPeriodeDeclaration)){
				final String cause = "le Rapport a une date de fin, postérieur à la date de fin de la période de déclaration:";
				aucunTraitement(cause, rapportAModifier, request);

			}else if(dateFin.isAfterOrEqual(dateDebutPeriodeDeclaration) && dateFin.isBefore(dateFinPeriodeDeclaration)){
				if (isEvenementFinRapportTravail(request)) {
					final String cause = "le Rapport a déjà une date de fin au %S :";
					aucunTraitement(cause, rapportAModifier, request);

				}else{
					reouvrirRapportTravail(rapportAModifier,request);
				}
			}
		}

	}

	private void reouvrirRapportTravail(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {


		loggerModification("Reouverture ",rapportAModifier);
		rapportAModifier.setDateFin(null);


	}

	/**Gere le cas ou on a une evenement de fermeture avec la presence d'un rapport de travail
	 *
	 * @param rapportAModifier rapport de travail trouvé pour la periode
	 * @param request la demande de modification
	 */
	private void handleEvenementFermetureRapportTravailExistant(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {
		final RegDate dateDebutRapportTravail = rapportAModifier.getDateDebut();
		final RegDate dateDebutPeriodeDeclaration = request.getDateDebutPeriodeDeclaration();

		if(dateDebutRapportTravail.isAfterOrEqual(dateDebutPeriodeDeclaration)){
			//CAS 18
			annulerRapportTravail(rapportAModifier);
		}else if (dateDebutRapportTravail.isBefore(dateDebutPeriodeDeclaration)) {
			//Traitement de la date de fin du rappport
			traiterFermetureRapportTravail(rapportAModifier,request);
		}
	}

	/**Applique les règles de fermeture du rapport de travail en cas d'évenement de fermeture (Z)
	 *
	 * @param rapportAModifier rapport de travail trouvé pour la periode
	 * @param request la demande de mis a jour
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
		if(dateFinRapport!=null && dateFinRapport.isBefore(request.getDateDebutPeriodeDeclaration())){
			final String cause = "Date de fin du rapport avant la date de début de la période";
			aucunTraitement(cause, rapportAModifier, request);
		}

	}

	/**Ferme un rapport de travail
	 *
	 * @param rapportAModifier rapport de travail à fermer
	 * @param dateFermeture la date de fin du rapport
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

	/**Effectue l'annulation du rapport de travail
	 *
	 * @param rapportAModifier  rapport de travail trouvé pour la periode
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
		//TODO (xsibnm) construire l'identifiant du rapport de travail
		//	response.setIdentifiantRapportTravail(request.getIdentifiantRapportTravail());
		response.setDatePriseEnCompte(DataHelper.coreToXML(RegDate.get()));
		return response;
	}

	/**
	 * Permet de traiter le cas ou le rapport de travail est inexistant
	 *
	 * @param dpi      le débiteur
	 * @param sourcier le sourcier
	 * @param request  la demande de mise à jour
	 */
	private void handleRapportPrestationInexistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, MiseAjourRapportTravail request) {


		//On a pas d'évènement de fermeture de rapport de travail
		if (!request.isFermetureRapportTravail()) {
			creerRapportTravail(dpi, sourcier, request);
		}
		else {
			//Si le rapport n'existe pas et qu 'on reçoit un evenement de fermeture, on ignore le message
			ignorerMessagePourRTInexistant(dpi, sourcier, request.getBusinessId());
		}
	}


	/**
	 * Trouve le rapport de Travail qui est concerné par la période de déclaration
	 *
	 *
	 *
	 * @param dpi                un débiteur
	 * @param sourcier           le sourcier
	 * @return le premier rapport de travail qui est concerne par la période de déclaration
	 */
	private List<RapportPrestationImposable> findRapportPrestationImposable(DebiteurPrestationImposable dpi, PersonnePhysique sourcier) {
		List<RapportPrestationImposable> listeRapport = tiersService.getAllRapportPrestationImposable(dpi, sourcier);
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

		//TODO(XSIBNM) rajouter la validation sur le for en fonction d ela date de dbut de versement de salaire
	}




	private void creerRapportTravail(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, MiseAjourRapportTravail request) {
		RegDate dateDebut = request.getDateDebutVersementSalaire();
		RegDate dateFin = calculerDateFinRapportTravail(request);


		tiersService.addRapportPrestationImposable(sourcier, dpi, dateDebut, dateFin, TypeActivite.PRINCIPALE, 100);

		final String message = String.format("Création d'un nouveau rapport de travail entre le débiteur %s et le sourcier %s." +
				"  Date début du rapport: %s, date de fin: %s", FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()),
				FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()),
				RegDateHelper.dateToDisplayString(dateDebut), RegDateHelper.dateToDisplayString(dateFin));
		LOGGER.info(message);

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

	private boolean isEvenementFinRapportTravail(MiseAjourRapportTravail request){
		return request.isDeces() || request.isSortie();
	}
	private void ignorerMessagePourRTInexistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, String businessId) {
		final String message = String.format("le message ayant comme business id %s et qui concerne le dbiteur %s et le sourcier %s." +
				"  sera ignoré car aucun rapport de travail n'a été trouvé pour cette demande de fermeture", businessId, FormatNumeroHelper.
				numeroCTBToDisplay(dpi.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()));
		LOGGER.info(message);
	}

	private void aucunTraitement(String cause, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {
		String message = String.format("Aucun traitement necessaire pour le message %s concernant le rapport de travail commencant" +
				" le %s, se terminant le %s pour le debiteur %s et le sourcier %s:",
				request.getBusinessId(),
				RegDateHelper.dateToDisplayString(rapportAModifier.getDateDebut()),
				RegDateHelper.dateToDisplayString(rapportAModifier.getDateFin()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getObjetId()),
				FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getSujetId()));
		LOGGER.info(message+": " + cause);
	}


	private boolean isRapportFermeAvantPeriodeDeclaration(RapportPrestationImposable rapport, MiseAjourRapportTravail request){
		final RegDate dateFin = rapport.getDateFin();
		return dateFin !=null && dateFin.isBefore(request.getDateDebutPeriodeDeclaration());
	}

	private boolean isRapportOuvertApresPeriode(RapportPrestationImposable rapport, MiseAjourRapportTravail request){
		final RegDate dateDebut = rapport.getDateDebut();
		return dateDebut !=null && dateDebut.isAfter(request.getDateFinPeriodeDeclaration());
	}

	private boolean isRapportSurPeriode(RapportPrestationImposable rapport, MiseAjourRapportTravail request){
		final DateRangeHelper.Range periodeDeclaration = new DateRangeHelper.Range(request.getDateDebutPeriodeDeclaration(),request.getDateFinPeriodeDeclaration());
		return DateRangeHelper.intersect(rapport,periodeDeclaration);
	}
}