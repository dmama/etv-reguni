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
		final RapportPrestationImposable rapportAModifier = findRapportPrestationImposable(dpi, sourcier, periodeDeclaration);

		if (rapportAModifier == null) {
			traiterRapportPrestationInexistant(dpi, sourcier, request);
		}
		else {
			traiterRapportPrestationExistant(dpi, sourcier, rapportAModifier, request);
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

	private void traiterRapportPrestationExistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {
		//On a un evenement de fermeture de rapport de travail
		if (request.isFermetureRapportTravail()) {
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
	}

	private void traiterFermetureRapportTravail(RapportPrestationImposable rapportAModifier, MiseAjourRapportTravail request) {

		final RegDate dateFermeture = request.getDateDebutPeriodeDeclaration().getOneDayBefore();
		final RegDate dateFinDeclaration = request.getDateFinPeriodeDeclaration();

		//cas 16 et cas 15
		final RegDate dateFinRapport = rapportAModifier.getDateFin();
		if (dateFinRapport == null || dateFinRapport.isBeforeOrEqual(dateFinDeclaration)) {
			rapportAModifier.setDateFin(dateFermeture);
			String message = String.format("Fermeture du rapport de travail commencant" +
					" le %s pour le debiteur %s et le sourcier %s. La date de fermeture a été determinée au %s",
					RegDateHelper.dateToDisplayString(rapportAModifier.getDateDebut()),
					FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getObjetId()),
					FormatNumeroHelper.numeroCTBToDisplay(rapportAModifier.getSujetId()),
					RegDateHelper.dateToDisplayString(dateFermeture));
			LOGGER.info(message);
		}
		//cas 17
		if(dateFinRapport!=null && dateFinRapport.isBefore(request.getDateDebutPeriodeDeclaration())){
			final String cause = "Date de fin du rapport avant la date de début de la période";
			aucunTraitement(cause,request.getBusinessId());
		}

	}

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
	private void traiterRapportPrestationInexistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, MiseAjourRapportTravail request) {


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
	 * @param dpi                un débiteur
	 * @param sourcier           le sourcier
	 * @param periodeDeclaration la période de déclaration
	 * @return le premier rapport de travail qui est concerne par la période de déclaration
	 */
	//TODO confirmer avc Christophe la régle de prise en compte du premier rapport valide
	private RapportPrestationImposable findRapportPrestationImposable(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, DateRange periodeDeclaration) {
		List<RapportPrestationImposable> listeRapport = tiersService.getRapportPrestationImposableForPeriode(dpi, sourcier, periodeDeclaration);
		if (listeRapport.size() > 1) {
			final String message = String.format("Plusieurs rapports de travail entre le débiteur %s et le sourcier %s ont été trouvés pour la période du  %s au %s." +
					"  Le premier de la liste est pris en compte",
					FormatNumeroHelper.numeroCTBToDisplay(dpi.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()),
					RegDateHelper.dateToDisplayString(periodeDeclaration.getDateDebut()), RegDateHelper.dateToDisplayString(periodeDeclaration.getDateFin()));
			LOGGER.info(message);
		}
		//Si La liste est vide on renvoie null sinon on renvoie le premier rapport d ela liste
		final RapportPrestationImposable rapport = listeRapport.isEmpty() ? null : listeRapport.get(0);
		return rapport;
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

	private void ignorerMessagePourRTInexistant(DebiteurPrestationImposable dpi, PersonnePhysique sourcier, String businessId) {
		final String message = String.format("le message ayant comme business id %s et qui concerne le dbiteur %s et le sourcier %s." +
				"  sera ignoré car aucun rapport de travail n'a été trouvé pour cette demande de fermeture", businessId, FormatNumeroHelper.
				numeroCTBToDisplay(dpi.getNumero()), FormatNumeroHelper.numeroCTBToDisplay(sourcier.getNumero()));
		LOGGER.info(message);
	}

	private void aucunTraitement(String cause,String businessId) {
		LOGGER.info(cause+": aucun traitement necessaire pour le message " + businessId);
	}
}
