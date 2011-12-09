package ch.vd.uniregctb.webservices.party3.impl;

import javax.annotation.Resource;
import javax.jws.WebParam;
import javax.jws.WebService;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationResponse;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsRequest;
import ch.vd.unireg.webservices.party3.AcknowledgeTaxDeclarationsResponse;
import ch.vd.unireg.webservices.party3.BatchParty;
import ch.vd.unireg.webservices.party3.BatchPartyEntry;
import ch.vd.unireg.webservices.party3.GetBatchPartyRequest;
import ch.vd.unireg.webservices.party3.GetDebtorInfoRequest;
import ch.vd.unireg.webservices.party3.GetModifiedTaxpayersRequest;
import ch.vd.unireg.webservices.party3.GetPartyRequest;
import ch.vd.unireg.webservices.party3.GetPartyTypeRequest;
import ch.vd.unireg.webservices.party3.GetTaxOfficesRequest;
import ch.vd.unireg.webservices.party3.GetTaxOfficesResponse;
import ch.vd.unireg.webservices.party3.PartyWebService;
import ch.vd.unireg.webservices.party3.SearchCorporationEventsRequest;
import ch.vd.unireg.webservices.party3.SearchCorporationEventsResponse;
import ch.vd.unireg.webservices.party3.SearchPartyRequest;
import ch.vd.unireg.webservices.party3.SearchPartyResponse;
import ch.vd.unireg.webservices.party3.SetAutomaticReimbursementBlockingRequest;
import ch.vd.unireg.webservices.party3.TaxDeclarationAcknowledgeCode;
import ch.vd.unireg.webservices.party3.WebServiceException;
import ch.vd.unireg.xml.common.v1.UserLogin;
import ch.vd.unireg.xml.exception.v1.AccessDeniedExceptionInfo;
import ch.vd.unireg.xml.exception.v1.BusinessExceptionCode;
import ch.vd.unireg.xml.exception.v1.ServiceExceptionInfo;
import ch.vd.unireg.xml.party.debtor.v1.DebtorInfo;
import ch.vd.unireg.xml.party.v1.Party;
import ch.vd.unireg.xml.party.v1.PartyType;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.webservices.common.LoadMonitorable;

/**
 * Cette classe réceptionne tous les appels au web-service, authentifie l'utilisateur, vérifie ses droits d'accès et finalement redirige les appels vers l'implémentation concrète du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@WebService(targetNamespace = "http://www.vd.ch/fiscalite/unireg/webservices/party3", serviceName = "PartyWebServiceFactory", portName = "Service",
		endpointInterface = "ch.vd.unireg.webservices.party3.PartyWebService")
public class PartyWebServiceEndPoint implements PartyWebService, LoadMonitorable {

	private static final Logger LOGGER = Logger.getLogger(PartyWebServiceEndPoint.class);
	private static final Logger READ_ACCESS = Logger.getLogger("party3.read");
	private static final Logger WRITE_ACCESS = Logger.getLogger("party3.write");

	/**
	 * Nombre d'appels actuellements en cours
	 */
	private final AtomicInteger load = new AtomicInteger(0);

	@Resource
	private WebServiceContext context;

	private PartyWebService service;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setService(PartyWebService service) {
		this.service = service;
	}

	@Override
	public int getLoad() {
		return load.intValue();
	}

	@Override
	public SearchPartyResponse searchParty(SearchPartyRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkLimitedReadAccess(params.getLogin());
			return service.searchParty(params);
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public PartyType getPartyType(GetPartyTypeRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			final PartyType type = service.getPartyType(params);
			if (type != null) {
				checkPartyReadAccess(params.getPartyNumber());
			}
			return type;
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Party getParty(GetPartyRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			final Party party = service.getParty(params);
			if (party != null) {
				checkPartyReadAccess(params.getPartyNumber());
				assertCoherence(params.getPartyNumber(), party.getNumber());
			}
			return party;
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	@Override
	public BatchParty getBatchParty(GetBatchPartyRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());

			BatchParty batch;

			if (params.getPartyNumbers() != null && params.getPartyNumbers().size() == 1) {
				// Cas particulier d'un seul numéro demandé, on dégrade gracieusement en getParty

				batch = new BatchParty();

				final Integer numero = params.getPartyNumbers().iterator().next();
				try {
					final GetPartyRequest p = new GetPartyRequest();
					p.setLogin(params.getLogin());
					p.setPartyNumber(numero);
					p.getParts().addAll(params.getParts());

					final Party party = service.getParty(p);
					if (party != null) {
						final BatchPartyEntry entry = new BatchPartyEntry();
						entry.setNumber(numero);
						entry.setParty(party);
						batch.getEntries().add(entry);
					}
				}
				catch (WebServiceException e) {
					final BatchPartyEntry entry = new BatchPartyEntry();
					entry.setNumber(numero);
					entry.setExceptionInfo(e.getFaultInfo());
					batch.getEntries().add(entry);
				}
			}
			else {
				// Cas général, on part en mode batch
				batch = service.getBatchParty(params);
			}

			if (batch != null) {
				checkBatchReadAccess(batch);
				checkBatchCoherence(batch);
				logEmbeddedExceptions(params, batch);
			}

			return batch;
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	@Override
	public GetTaxOfficesResponse getTaxOffices(@WebParam(partName = "getTaxOfficesRequest", name = "getTaxOfficesRequest",
			targetNamespace = "http://www.vd.ch/fiscalite/unireg/webservices/party3") GetTaxOfficesRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			return service.getTaxOffices(params);
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	@Override
	public void setAutomaticReimbursementBlocking(SetAutomaticReimbursementBlockingRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			checkPartyWriteAccess(params.getPartyNumber());
			service.setAutomaticReimbursementBlocking(params);
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logWriteAccess(params, end - start);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public SearchCorporationEventsResponse searchCorporationEvents(SearchCorporationEventsRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			// Note : il n'y a pas de contrôle d'accès sur les PMs.
			return service.searchCorporationEvents(params);
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	@Override
	public DebtorInfo getDebtorInfo(GetDebtorInfoRequest params) throws
			WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			final DebtorInfo info = service.getDebtorInfo(params);
			if (info != null) {
				checkPartyReadAccess(params.getDebtorNumber());
				assertCoherence(params.getDebtorNumber(), info.getNumber());
			}
			return info;
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	@Override
	public AcknowledgeTaxDeclarationsResponse acknowledgeTaxDeclarations(AcknowledgeTaxDeclarationsRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());

			if (!SecurityProvider.isGranted(Role.DI_QUIT_PP, params.getLogin().getUserId(), params.getLogin().getOid())) {
				throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + params.getLogin().getUserId() + '/' + params.getLogin().getOid() +
						") n'a pas les droits de quittancement des déclarations d'impôt ordinaires sur l'application.");
			}

			final AcknowledgeTaxDeclarationsResponse reponses = service.acknowledgeTaxDeclarations(params);
			logEmbeddedErrors(params, reponses);
			return reponses;
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logWriteAccess(params, end - start);
		}
	}

	@Override
	public void ping() {
		// rien à faire
	}

	@Override
	public Integer[] getModifiedTaxpayers(GetModifiedTaxpayersRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			return service.getModifiedTaxpayers(params);
		}
		catch (WebServiceException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw ExceptionHelper.newTechnicalException(e.getMessage());
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	/**
	 * Login l'utilisateur dans l'application.
	 *
	 * @param login le login de l'utilisateur
	 * @throws ch.vd.uniregctb.webservices.party3.WebServiceException
	 *          si le login n'est pas renseigné convenablement.
	 */
	private void login(UserLogin login) throws WebServiceException {

		// un nouvel appel est en train de débuter
		load.incrementAndGet();

		if (login == null || login.getUserId() == null || login.getOid() == 0 || login.getUserId().trim().isEmpty()) {
			throw ExceptionHelper.newBusinessException("L'identification de l'utilisateur (userId + oid) doit être renseignée.", BusinessExceptionCode.INVALID_REQUEST);
		}

		AuthenticationHelper.setPrincipal(login.getUserId(), login.getOid());
	}

	/**
	 * Logout l'utilisateur de l'application
	 */
	private void logout() {
		AuthenticationHelper.resetAuthentication();

		// tout est fini
		load.decrementAndGet();
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture limités ou complete sur l'application.
	 *
	 * @param login l'information de login de l'utilisareur
	 * @throws ch.vd.uniregctb.webservices.party3.WebServiceException
	 *          si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkLimitedReadAccess(UserLogin login) throws WebServiceException {
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid()) &&
				!SecurityProvider.isGranted(Role.VISU_LIMITE, login.getUserId(), login.getOid())) {
			throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid()
					+ ") n'a pas les droits d'accès en lecture sur l'application.");
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur l'application en général.
	 *
	 * @param login l'information de login de l'utilisareur
	 * @throws ch.vd.uniregctb.webservices.party3.WebServiceException
	 *          si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkGeneralReadAccess(UserLogin login) throws WebServiceException {
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + login.getUserId() + '/' + login.getOid()
					+ ") n'a pas les droits d'accès en lecture complète sur l'application.");
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur le tiers spécifié.
	 *
	 * @param partyId le tiers sur lequel on veut vérifier les droits d'accès
	 * @throws ch.vd.uniregctb.webservices.party3.WebServiceException
	 *          si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkPartyReadAccess(long partyId) throws WebServiceException {
		final Niveau acces = SecurityProvider.getDroitAcces(partyId);
		if (acces == null) {
			throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + '/'
					+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + partyId);
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur le batch de tiers spécifié. Dans le cas contraire, le pointeur vers le tiers correspondant est annulé et un message
	 * d'exception est renseigné.
	 *
	 * @param batch le batch de tiers sur lequel on veut vérifier les droits d'accès
	 */
	private void checkBatchReadAccess(BatchParty batch) {
		final int size = batch.getEntries().size();

		final List<Long> ids = new ArrayList<Long>();
		for (BatchPartyEntry e : batch.getEntries()) {
			if (e.getParty() == null) {
				ids.add(null);
			}
			else {
				ids.add((long) e.getNumber());
			}
		}
		Assert.isTrue(ids.size() == size);

		final List<Niveau> niveaux = SecurityProvider.getDroitsAcces(ids);
		Assert.isTrue(niveaux.size() == size);

		for (int i = 0; i < ids.size(); ++i) {
			final BatchPartyEntry entry = batch.getEntries().get(i);
			if (entry.getParty() == null) {
				continue;
			}
			final Niveau niveau = niveaux.get(i);
			if (niveau == null) {
				String message = "L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + '/'
						+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + entry.getNumber();
				entry.setParty(null);
				entry.setExceptionInfo(new AccessDeniedExceptionInfo(message, null));
			}
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture et écriture sur le tiers spécifié.
	 *
	 * @param partyId le tiers sur lequel on veut vérifier les droits d'accès
	 * @throws ch.vd.uniregctb.webservices.party3.WebServiceException
	 *          si l'utilisateur courant ne possède pas les droits de lecture et écriture
	 */
	private static void checkPartyWriteAccess(long partyId) throws WebServiceException {
		final Niveau acces = SecurityProvider.getDroitAcces(partyId);
		if (acces == null || acces == Niveau.LECTURE) {
			throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + '/'
					+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en écriture sur le tiers n° " + partyId);
		}
	}

	/**
	 * Vérifie que l'id du tiers retourné corresponds bien à celui demandé.
	 *
	 * @param expected l'id demandé
	 * @param actual   l'id retourné
	 * @throws ch.vd.uniregctb.webservices.party3.WebServiceException
	 *          si les deux ids ne sont pas égaux.
	 */
	private void assertCoherence(long expected, long actual) throws WebServiceException {
		if (expected != actual) {
			throw ExceptionHelper.newTechnicalException(String.format(
					"Incohérence des données retournées détectées: tiers demandé = %d, tiers retourné = %d.", expected, actual));
		}
	}

	/**
	 * Vérifie que l'id de chaque tiers retourné corresponds bien à celui demandé.
	 *
	 * @param batch le batch à vérifier
	 * @throws ch.vd.uniregctb.webservices.party3.WebServiceException
	 *          si les ids retournés ne correspondent pas à ceux demandés.
	 */
	private void checkBatchCoherence(BatchParty batch) throws WebServiceException {
		for (BatchPartyEntry e : batch.getEntries()) {
			if (e.getParty() != null) {
				assertCoherence(e.getNumber(), e.getParty().getNumber());
			}
		}
	}

	/**
	 * Log en erreur les exceptions embeddées dans le batch spécifié.
	 *
	 * @param params le message initial
	 * @param batch  les données retournées
	 */
	private void logEmbeddedExceptions(GetBatchPartyRequest params, BatchParty batch) {

		List<BatchPartyEntry> inError = null;

		for (BatchPartyEntry entry : batch.getEntries()) {
			if (entry.getExceptionInfo() != null) {
				if (inError == null) {
					inError = new ArrayList<BatchPartyEntry>();
				}
				inError.add(entry);
			}
		}

		if (inError != null) {
			StringBuilder message = new StringBuilder();
			message.append("Les exceptions suivantes ont été levées lors du traitement du message ").append(params).append(" : ");
			for (BatchPartyEntry entry : inError) {
				message.append("\n - id=").append(entry.getNumber());
				final ServiceExceptionInfo exceptionInfo = entry.getExceptionInfo();
				message.append(", exception=\"").append(exceptionInfo.getMessage());
				message.append("\", type=").append(exceptionInfo.getClass().getSimpleName());
			}
			LOGGER.error(message.toString());
		}
	}

	/**
	 * Log en erreur les erreurs rencontrées dans les demandes de quittancement
	 *
	 * @param params   le message de demande de quittancements
	 * @param reponses les données retournées
	 */
	private void logEmbeddedErrors(AcknowledgeTaxDeclarationsRequest params, AcknowledgeTaxDeclarationsResponse reponses) {

		// 1. collection des cas en erreur
		List<AcknowledgeTaxDeclarationResponse> inError = null;
		for (AcknowledgeTaxDeclarationResponse reponse : reponses.getResponses()) {
			if (reponse.getCode() != TaxDeclarationAcknowledgeCode.OK) {
				if (inError == null) {
					inError = new ArrayList<AcknowledgeTaxDeclarationResponse>();
				}
				inError.add(reponse);
			}
		}

		// 2. log des erreurs
		if (inError != null) {
			final StringBuilder b = new StringBuilder();
			b.append("Les erreurs suivantes ont été levées lors du traitement du message ").append(params).append(" : ");
			for (AcknowledgeTaxDeclarationResponse reponse : inError) {
				b.append("\n - key=").append(reponse.getKey());
				b.append(", code=").append(reponse.getCode());
				if (reponse.getCode() == TaxDeclarationAcknowledgeCode.EXCEPTION) {
					final ServiceExceptionInfo exceptionInfo = reponse.getExceptionInfo();
					b.append(", exception=\"").append(exceptionInfo.getMessage()).append("\", type=").append(exceptionInfo.getClass().getSimpleName());
				}
			}
			LOGGER.error(b.toString());
		}
	}

	/**
	 * Log les paramètres et la durée d'un appel en read-only
	 *
	 * @param params   les paramètres de l'appel
	 * @param duration la durée de l'appel en nano-secondes
	 */
	private void logReadAccess(Object params, long duration) {
		if (READ_ACCESS.isInfoEnabled()) {
			final String user = getBasicAuthenticationUser();

			// appelsEnCours+1 : +1 car le logout a déjà été fait quand on arrive ici et l'appel courant a donc été décompté
			READ_ACCESS.info(String.format("[%s] (%d ms) %s load=%d", user, duration / 1000000, params.toString(), load.get() + 1));
		}
	}

	/**
	 * Log les paramètres et la durée d'un appel en read-write
	 *
	 * @param params   les paramètres de l'appel
	 * @param duration la durée de l'appel en nano-secondes
	 */
	private void logWriteAccess(Object params, long duration) {
		if (WRITE_ACCESS.isInfoEnabled()) {
			final String user = getBasicAuthenticationUser();

			// appelsEnCours+1 : +1 car le logout a déjà été fait quand on arrive ici et l'appel courant a donc été décompté
			WRITE_ACCESS.info(String.format("[%s] (%d ms) %s load=%d", user, duration / 1000000, params.toString(), load.get() + 1));
		}
	}

	/**
	 * @return le nom de l'utilisateur utilisé pour se connecter au web-service en mode <i>basic authentication</i>; ou "n/a" si cette information n'existe pas.
	 */
	private String getBasicAuthenticationUser() {
		final MessageContext ctx = (context == null ? null : context.getMessageContext());
		final HttpServletRequest request = (ctx == null ? null : (HttpServletRequest) ctx.get(AbstractHTTPDestination.HTTP_REQUEST));
		final Principal userPrincipal = (request == null ? null : request.getUserPrincipal());
		return (userPrincipal == null ? "n/a" : userPrincipal.getName());
	}
}
