package ch.vd.uniregctb.webservices.tiers3.impl;

import javax.annotation.Resource;
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

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.webservices.common.LoadMonitorable;
import ch.vd.uniregctb.webservices.tiers3.AccessDeniedExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.BatchTiers;
import ch.vd.uniregctb.webservices.tiers3.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers3.BusinessExceptionInfo;
import ch.vd.uniregctb.webservices.tiers3.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers3.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers3.GetBatchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.GetDebiteurInfoRequest;
import ch.vd.uniregctb.webservices.tiers3.GetListeCtbModifiesRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.GetTiersTypeRequest;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsRequest;
import ch.vd.uniregctb.webservices.tiers3.QuittancerDeclarationsResponse;
import ch.vd.uniregctb.webservices.tiers3.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers3.SearchEvenementsPMRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchEvenementsPMResponse;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersRequest;
import ch.vd.uniregctb.webservices.tiers3.SearchTiersResponse;
import ch.vd.uniregctb.webservices.tiers3.SetTiersBlocRembAutoRequest;
import ch.vd.uniregctb.webservices.tiers3.Tiers;
import ch.vd.uniregctb.webservices.tiers3.TiersWebService;
import ch.vd.uniregctb.webservices.tiers3.TypeTiers;
import ch.vd.uniregctb.webservices.tiers3.TypeWebServiceException;
import ch.vd.uniregctb.webservices.tiers3.UserLogin;
import ch.vd.uniregctb.webservices.tiers3.WebServiceException;

/**
 * Cette classe réceptionne tous les appels au web-service, authentifie l'utilisateur, vérifie ses droits d'accès et finalement redirige les appels vers l'implémentation concrète du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@WebService(targetNamespace = "http://www.vd.ch/unireg/webservices/tiers3", serviceName = "TiersWebServiceFactory", portName = "Service", endpointInterface = "ch.vd.uniregctb.webservices.tiers3.TiersWebService")
public class TiersWebServiceEndPoint implements TiersWebService, LoadMonitorable {

	private static final Logger LOGGER = Logger.getLogger(TiersWebServiceEndPoint.class);
	private static final Logger READ_ACCESS = Logger.getLogger("tiers3.read");
	private static final Logger WRITE_ACCESS = Logger.getLogger("tiers3.write");

	/**
	 * Nombre d'appels actuellements en cours
	 */
	private final AtomicInteger appelsEnCours = new AtomicInteger(0);

	@Resource
	private WebServiceContext context;

	private TiersWebService service;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setService(TiersWebService service) {
		this.service = service;
	}

	public int getLoad() {
		return appelsEnCours.intValue();
	}

	@Override
	public SearchTiersResponse searchTiers(SearchTiersRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkLimitedReadAccess(params.getLogin());
			return service.searchTiers(params);
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
	public TypeTiers getTiersType(GetTiersTypeRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			final TypeTiers type = service.getTiersType(params);
			if (type != null) {
				checkTiersReadAccess(params.getTiersNumber());
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
	public Tiers getTiers(GetTiersRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			final Tiers tiers = service.getTiers(params);
			if (tiers != null) {
				checkTiersReadAccess(params.getTiersNumber());
				assertCoherence(params.getTiersNumber(), tiers.getNumero());
			}
			return tiers;
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

	public BatchTiers getBatchTiers(GetBatchTiersRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());

			BatchTiers batch;

			if (params.getTiersNumbers() != null && params.getTiersNumbers().size() == 1) {
				// Cas particulier d'un seul numéro demandé, on dégrade gracieusement en getTiers

				batch = new BatchTiers();

				final Long numero = params.getTiersNumbers().iterator().next();
				try {
					final GetTiersRequest p = new GetTiersRequest();
					p.setLogin(params.getLogin());
					p.setTiersNumber(numero);
					p.getParts().addAll(params.getParts());

					final Tiers tiers = service.getTiers(p);
					if (tiers != null) {
						final BatchTiersEntry entry = new BatchTiersEntry();
						entry.setNumber(numero);
						entry.setTiers(tiers);
						batch.getEntries().add(entry);
					}
				}
				catch (WebServiceException e) {
					final BatchTiersEntry entry = new BatchTiersEntry();
					entry.setNumber(numero);
					entry.setExceptionMessage(e.getMessage());
					if (e.getFaultInfo() instanceof AccessDeniedExceptionInfo) {
						entry.setExceptionType(TypeWebServiceException.ACCESS_DENIED);
					}
					else if (e.getFaultInfo() instanceof BusinessExceptionInfo) {
						entry.setExceptionType(TypeWebServiceException.BUSINESS);
					}
					else {
						entry.setExceptionType(TypeWebServiceException.TECHNICAL);
					}
					batch.getEntries().add(entry);
				}
			}
			else {
				// Cas général, on part en mode batch
				batch = service.getBatchTiers(params);
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

	public void setTiersBlocRembAuto(SetTiersBlocRembAutoRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			checkTiersWriteAccess(params.getTiersNumber());
			service.setTiersBlocRembAuto(params);
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
	public SearchEvenementsPMResponse searchEvenementsPM(SearchEvenementsPMRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			// Note : il n'y a pas de contrôle d'accès sur les PMs.
			return service.searchEvenementsPM(params);
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

	public DebiteurInfo getDebiteurInfo(GetDebiteurInfoRequest params) throws
			WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			final DebiteurInfo info = service.getDebiteurInfo(params);
			if (info != null) {
				checkTiersReadAccess(params.getNumeroDebiteur());
				assertCoherence(params.getNumeroDebiteur(), info.getNumeroDebiteur());
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

	public QuittancerDeclarationsResponse quittancerDeclarations(QuittancerDeclarationsRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());

			if (!SecurityProvider.isGranted(Role.DI_QUIT_PP, params.getLogin().getUserId(), params.getLogin().getOid())) {
				throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + params.getLogin().getUserId() + "/" + params.getLogin().getOid() +
						") n'a pas les droits de quittancement des déclarations d'impôt ordinaires sur l'application.");
			}

			final QuittancerDeclarationsResponse reponses = service.quittancerDeclarations(params);
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
	public Long[] getListeCtbModifies(GetListeCtbModifiesRequest params) throws WebServiceException {
		final long start = System.nanoTime();
		try {
			login(params.getLogin());
			checkGeneralReadAccess(params.getLogin());
			return service.getListeCtbModifies(params);
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
	 * @throws ch.vd.uniregctb.webservices.tiers3.WebServiceException
	 *          si le login n'est pas renseigné convenablement.
	 */
	private void login(UserLogin login) throws WebServiceException {

		// un nouvel appel est en train de débuter
		appelsEnCours.incrementAndGet();

		if (login == null || login.getUserId() == null || login.getOid() == 0 || login.getUserId().trim().equals("")) {
			throw ExceptionHelper.newBusinessException("L'identification de l'utilisateur (userId + oid) doit être renseignée.");
		}

		AuthenticationHelper.setPrincipal(login.getUserId());
		AuthenticationHelper.setCurrentOID(login.getOid());
	}

	/**
	 * Logout l'utilisateur de l'application
	 */
	private void logout() {
		AuthenticationHelper.resetAuthentication();

		// tout est fini
		appelsEnCours.decrementAndGet();
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture limités ou complete sur l'application.
	 *
	 * @param login l'information de login de l'utilisareur
	 * @throws ch.vd.uniregctb.webservices.tiers3.WebServiceException
	 *          si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkLimitedReadAccess(UserLogin login) throws WebServiceException {
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid()) &&
				!SecurityProvider.isGranted(Role.VISU_LIMITE, login.getUserId(), login.getOid())) {
			throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + login.getUserId() + "/" + login.getOid()
					+ ") n'a pas les droits d'accès en lecture sur l'application.");
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur l'application en général.
	 *
	 * @param login l'information de login de l'utilisareur
	 * @throws ch.vd.uniregctb.webservices.tiers3.WebServiceException
	 *          si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkGeneralReadAccess(UserLogin login) throws WebServiceException {
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.getUserId(), login.getOid())) {
			throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + login.getUserId() + "/" + login.getOid()
					+ ") n'a pas les droits d'accès en lecture complète sur l'application.");
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur le tiers spécifié.
	 *
	 * @param tiersId le tiers sur lequel on veut vérifier les droits d'accès
	 * @throws ch.vd.uniregctb.webservices.tiers3.WebServiceException
	 *          si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkTiersReadAccess(long tiersId) throws WebServiceException {
		final Niveau acces = SecurityProvider.getDroitAcces(tiersId);
		if (acces == null) {
			throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + "/"
					+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + tiersId);
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur le batch de tiers spécifié. Dans le cas contraire, le pointeur vers le tiers correspondant est annulé et un message
	 * d'exception est renseigné.
	 *
	 * @param batch le batch de tiers sur lequel on veut vérifier les droits d'accès
	 */
	private void checkBatchReadAccess(BatchTiers batch) {
		final int size = batch.getEntries().size();

		final List<Long> ids = new ArrayList<Long>();
		for (BatchTiersEntry e : batch.getEntries()) {
			if (e.getTiers() == null) {
				ids.add(null);
			}
			else {
				ids.add(e.getNumber());
			}
		}
		Assert.isTrue(ids.size() == size);

		final List<Niveau> niveaux = SecurityProvider.getDroitsAcces(ids);
		Assert.isTrue(niveaux.size() == size);

		for (int i = 0; i < ids.size(); ++i) {
			final BatchTiersEntry entry = batch.getEntries().get(i);
			if (entry.getTiers() == null) {
				continue;
			}
			final Niveau niveau = niveaux.get(i);
			if (niveau == null) {
				String message = "L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + "/"
						+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + entry.getNumber();
				entry.setTiers(null);
				entry.setExceptionMessage(message);
				entry.setExceptionType(TypeWebServiceException.ACCESS_DENIED);
			}
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture et écriture sur le tiers spécifié.
	 *
	 * @param tiersId le tiers sur lequel on veut vérifier les droits d'accès
	 * @throws ch.vd.uniregctb.webservices.tiers3.WebServiceException
	 *          si l'utilisateur courant ne possède pas les droits de lecture et écriture
	 */
	private static void checkTiersWriteAccess(long tiersId) throws WebServiceException {
		final Niveau acces = SecurityProvider.getDroitAcces(tiersId);
		if (acces == null || acces == Niveau.LECTURE) {
			throw ExceptionHelper.newAccessDeniedException("L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + "/"
					+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en écriture sur le tiers n° " + tiersId);
		}
	}

	/**
	 * Vérifie que l'id du tiers retourné corresponds bien à celui demandé.
	 *
	 * @param expected l'id demandé
	 * @param actual   l'id retourné
	 * @throws ch.vd.uniregctb.webservices.tiers3.WebServiceException
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
	 * @throws ch.vd.uniregctb.webservices.tiers3.WebServiceException
	 *          si les ids retournés ne correspondent pas à ceux demandés.
	 */
	private void checkBatchCoherence(BatchTiers batch) throws WebServiceException {
		for (BatchTiersEntry e : batch.getEntries()) {
			if (e.getTiers() != null) {
				assertCoherence(e.getNumber(), e.getTiers().getNumero());
			}
		}
	}

	/**
	 * Log en erreur les exceptions embeddées dans le batch spécifié.
	 *
	 * @param params le message initial
	 * @param batch  les données retournées
	 */
	private void logEmbeddedExceptions(GetBatchTiersRequest params, BatchTiers batch) {

		List<BatchTiersEntry> inError = null;

		for (BatchTiersEntry entry : batch.getEntries()) {
			if (entry.getExceptionMessage() != null) {
				if (inError == null) {
					inError = new ArrayList<BatchTiersEntry>();
				}
				inError.add(entry);
			}
		}

		if (inError != null) {
			StringBuilder message = new StringBuilder();
			message.append("Les exceptions suivantes ont été levées lors du traitement du message ").append(params).append(" : ");
			for (BatchTiersEntry entry : inError) {
				message.append("\n - id=").append(entry.getNumber());
				message.append(", exception=\"").append(entry.getExceptionMessage());
				message.append("\", type=").append(entry.getExceptionType());
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
	private void logEmbeddedErrors(QuittancerDeclarationsRequest params, QuittancerDeclarationsResponse reponses) {

		// 1. collection des cas en erreur
		List<ReponseQuittancementDeclaration> inError = null;
		for (ReponseQuittancementDeclaration reponse : reponses.getItem()) {
			if (reponse.getCode() != CodeQuittancement.OK) {
				if (inError == null) {
					inError = new ArrayList<ReponseQuittancementDeclaration>();
				}
				inError.add(reponse);
			}
		}

		// 2. log des erreurs
		if (inError != null) {
			final StringBuilder b = new StringBuilder();
			b.append("Les erreurs suivantes ont été levées lors du traitement du message ").append(params).append(" : ");
			for (ReponseQuittancementDeclaration reponse : inError) {
				b.append("\n - key=").append(reponse.getKey());
				b.append(", code=").append(reponse.getCode());
				if (reponse.getCode() == CodeQuittancement.EXCEPTION) {
					b.append(", exception=\"").append(reponse.getExceptionMessage()).append("\", type=").append(reponse.getExceptionType());
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
			READ_ACCESS.info(String.format("[%s] (%d ms) %s load=%d", user, duration / 1000000, params.toString(), appelsEnCours.get() + 1));
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
			WRITE_ACCESS.info(String.format("[%s] (%d ms) %s load=%d", user, duration / 1000000, params.toString(), appelsEnCours.get() + 1));
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
