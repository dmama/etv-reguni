package ch.vd.uniregctb.webservices.tiers2.impl;

import javax.annotation.Resource;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
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
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiers;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersEntry;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.BatchTiersHistoEntry;
import ch.vd.uniregctb.webservices.tiers2.data.CodeQuittancement;
import ch.vd.uniregctb.webservices.tiers2.data.DebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.data.EvenementPM;
import ch.vd.uniregctb.webservices.tiers2.data.ReponseQuittancementDeclaration;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers.Type;
import ch.vd.uniregctb.webservices.tiers2.data.TiersHisto;
import ch.vd.uniregctb.webservices.tiers2.data.TiersId;
import ch.vd.uniregctb.webservices.tiers2.data.TiersInfo;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceException;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceExceptionType;
import ch.vd.uniregctb.webservices.tiers2.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetBatchTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetDebiteurInfo;
import ch.vd.uniregctb.webservices.tiers2.params.GetListeCtbModifies;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers2.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers2.params.QuittancerDeclarations;
import ch.vd.uniregctb.webservices.tiers2.params.SearchEvenementsPM;
import ch.vd.uniregctb.webservices.tiers2.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers2.params.SetTiersBlocRembAuto;

/**
 * Cette classe réceptionne tous les appels au web-service, authentifie l'utilisateur, vérifie ses droits d'accès et finalement redirige les
 * appels vers l'implémentation concrète du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", name = "TiersPort", serviceName = "TiersService")
public class TiersWebServiceEndPoint implements TiersWebService, LoadMonitorable {

	private static final Logger LOGGER = Logger.getLogger(TiersWebServiceEndPoint.class);
	private static final Logger READ_ACCESS = Logger.getLogger("tiers2.read");
	private static final Logger WRITE_ACCESS = Logger.getLogger("tiers2.write");

	/**
	 * Nombre d'appels actuellements en cours
	 */
	private final AtomicInteger appelsEnCours = new AtomicInteger(0);

	@Resource
	private WebServiceContext context;

	private TiersWebService service;

	public void setService(TiersWebService service) {
		this.service = service;
	}

	public int getLoad() {
		return appelsEnCours.intValue();
	}

	/**
	 * {@inheritDoc}
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public List<TiersInfo> searchTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "SearchTiers") SearchTiers params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkLimitedReadAccess(params.login);
			return service.searchTiers(params);
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
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
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public Tiers.Type getTiersType(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetTiersType") GetTiersType params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			final Type type = service.getTiersType(params);
			if (type != null) {
				checkTiersReadAccess(params.tiersNumber);
			}
			return type;
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
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
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public Tiers getTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetTiers") GetTiers params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			final Tiers tiers = service.getTiers(params);
			if (tiers != null) {
				checkTiersReadAccess(params.tiersNumber);
				assertCoherence(params.tiersNumber, tiers.numero);
			}
			return tiers;
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
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
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public TiersHisto getTiersPeriode(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetTiersPeriode") GetTiersPeriode params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			final TiersHisto tiers = service.getTiersPeriode(params);
			if (tiers != null) {
				checkTiersReadAccess(params.tiersNumber);
				assertCoherence(params.tiersNumber, tiers.numero);
			}
			return tiers;
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
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
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public TiersHisto getTiersHisto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetTiersHisto") GetTiersHisto params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			final TiersHisto tiers = service.getTiersHisto(params);
			if (tiers != null) {
				checkTiersReadAccess(params.tiersNumber);
				assertCoherence(params.tiersNumber, tiers.numero);
			}
			return tiers;
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public BatchTiers getBatchTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetBatchTiers") GetBatchTiers params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);

			BatchTiers batch;
			
			if (params.tiersNumbers != null && params.tiersNumbers.size() == 1) {
				// Cas particulier d'un seul numéro demandé, on dégrade gracieusement en getTiers
				final Long numero = params.tiersNumbers.iterator().next();
				try {
					final Tiers tiers = service.getTiers(new GetTiers(params.login, numero, params.date, params.parts));
					if (tiers == null) {
						batch = new BatchTiers();
					}
					else {
						batch = new BatchTiers(new BatchTiersEntry(numero, tiers));
					}
				}
				catch (WebServiceException e) {
					batch = new BatchTiers(new BatchTiersEntry(numero, e));
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
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public BatchTiersHisto getBatchTiersHisto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetBatchTiersHisto") GetBatchTiersHisto params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);

			BatchTiersHisto batch;

			if (params.tiersNumbers != null && params.tiersNumbers.size() == 1) {
				// Cas particulier d'un seul numéro demandé, on dégrade gracieusement en getTiersHisto
				final Long numero = params.tiersNumbers.iterator().next();
				try {
					final TiersHisto tiers = service.getTiersHisto(new GetTiersHisto(params.login, numero, params.parts));
					if (tiers == null) {
						batch = new BatchTiersHisto();
					}
					else {
						batch = new BatchTiersHisto(new BatchTiersHistoEntry(numero, tiers));
					}
				}
				catch (WebServiceException e) {
					batch = new BatchTiersHisto(new BatchTiersHistoEntry(numero, e));
				}
			}
			else {
				// Cas général, on part en mode batch
				batch = service.getBatchTiersHisto(params);
			}

			if (batch != null) {
				checkBatchReadAccess(batch);
				checkBatchCoherence(batch);
				logEmbeddedExceptions(params, batch);
			}

			return batch;
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
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
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public void setTiersBlocRembAuto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "SetTiersBlocRembAuto") SetTiersBlocRembAuto params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			checkTiersWriteAccess(params.tiersNumber);
			service.setTiersBlocRembAuto(params);
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
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
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public List<EvenementPM> searchEvenementsPM(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "SearchEvenementsPM") SearchEvenementsPM params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			// Note : il n'y a pas de contrôle d'accès sur les PMs.
			return service.searchEvenementsPM(params);
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public DebiteurInfo getDebiteurInfo(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetDebiteurInfo") GetDebiteurInfo params) throws
			BusinessException, AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			final DebiteurInfo info = service.getDebiteurInfo(params);
			if (info != null) {
				checkTiersReadAccess(params.numeroDebiteur);
				assertCoherence(params.numeroDebiteur, info.numeroDebiteur);
			}
			return info;
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public List<ReponseQuittancementDeclaration> quittancerDeclarations(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "QuittancerDeclarations") QuittancerDeclarations params) throws BusinessException,
			AccessDeniedException, TechnicalException {
		final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);

			if (!SecurityProvider.isGranted(Role.DI_QUIT_PP, params.login.userId, params.login.oid)) {
				throw new AccessDeniedException(
						"L'utilisateur spécifié (" + params.login.userId + "/" + params.login.oid + ") n'a pas les droits de quittancement des déclarations d'impôt ordinaires sur l'application.");
			}

			final List<ReponseQuittancementDeclaration> reponses = service.quittancerDeclarations(params);
			logEmbeddedErrors(params, reponses);
			return reponses;
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logWriteAccess(params, end - start);
		}
	}

	@Override
	public List<TiersId> getListeCtbModifies(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetListeCtbModifies") GetListeCtbModifies params) throws BusinessException,
			AccessDeniedException, TechnicalException {
	final long start = System.nanoTime();
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			final List<TiersId> listIds = service.getListeCtbModifies(params);
			return listIds;
		}
		catch (BusinessException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (AccessDeniedException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (TechnicalException e) {
			LOGGER.error("Exception lors du traitement du message " + params + " : " + e.getMessage());
			throw e;
		}
		catch (RuntimeException e) {
			LOGGER.error("Exception lors du traitement du message " + params, e);
			throw new TechnicalException(e);
		}
		finally {
			logout();
			final long end = System.nanoTime();
			logReadAccess(params, end - start);
		}
	}

	/**
	 * Cette méthode s'assure que les classes concrètes dérivant de Tiers sont exposées dans le WSDL. Elle ne fait rien proprement dit.
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public void doNothing(AllConcreteTiersClasses dummy) {
	}

	/**
	 * Login l'utilisateur dans l'application.
	 *
	 * @param login le login de l'utilisateur
	 * @throws ch.vd.uniregctb.webservices.tiers2.exception.BusinessException
	 *          si le login n'est pas renseigné convenablement.
	 */
	private void login(UserLogin login) throws BusinessException {

		// un nouvel appel est en train de débuter
		appelsEnCours.incrementAndGet();

		if (login == null || login.userId == null || login.oid == null || login.userId.trim().equals("")) {
			throw new BusinessException("L'identification de l'utilisateur (userId + oid) doit être renseignée.");
		}

		AuthenticationHelper.setPrincipal(login.userId);
		AuthenticationHelper.setCurrentOID(login.oid);
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
	 * @throws AccessDeniedException si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkLimitedReadAccess(UserLogin login) throws AccessDeniedException {
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.userId, login.oid) &&
				!SecurityProvider.isGranted(Role.VISU_LIMITE, login.userId, login.oid)) {
			throw new AccessDeniedException("L'utilisateur spécifié (" + login.userId + "/" + login.oid
					+ ") n'a pas les droits d'accès en lecture sur l'application.");
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur l'application en général.
	 *
	 * @param login l'information de login de l'utilisareur
	 * @throws AccessDeniedException si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkGeneralReadAccess(UserLogin login) throws AccessDeniedException {
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.userId, login.oid)) {
			throw new AccessDeniedException("L'utilisateur spécifié (" + login.userId + "/" + login.oid
					+ ") n'a pas les droits d'accès en lecture complète sur l'application.");
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur le tiers spécifié.
	 *
	 * @param tiersId le tiers sur lequel on veut vérifier les droits d'accès
	 * @throws AccessDeniedException si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkTiersReadAccess(long tiersId) throws AccessDeniedException {
		final Niveau acces = SecurityProvider.getDroitAcces(tiersId);
		if (acces == null) {
			throw new AccessDeniedException("L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + "/"
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
		final int size = batch.entries.size();

		final List<Long> ids = new ArrayList<Long>();
		for (BatchTiersEntry e : batch.entries) {
			if (e.tiers == null) {
				ids.add(null);
			}
			else {
				ids.add(e.number);
			}
		}
		Assert.isTrue(ids.size() == size);

		final List<Niveau> niveaux = SecurityProvider.getDroitsAcces(ids);
		Assert.isTrue(niveaux.size() == size);

		for (int i = 0; i < ids.size(); ++i) {
			final BatchTiersEntry entry = batch.entries.get(i);
			if (entry.tiers == null) {
				continue;
			}
			final Niveau niveau = niveaux.get(i);
			if (niveau == null) {
				String message = "L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + "/"
						+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + entry.number;
				entry.tiers = null;
				entry.exceptionMessage = message;
				entry.exceptionType = WebServiceExceptionType.ACCESS_DENIED;
			}
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur le batch de tiers spécifié. Dans le cas contraire, le pointeur vers le tiers correspondant est annulé et un message
	 * d'exception est renseigné.
	 *
	 * @param batch le batch de tiers sur lequel on veut vérifier les droits d'accès
	 */
	private void checkBatchReadAccess(BatchTiersHisto batch) {
		final int size = batch.entries.size();

		final List<Long> ids = new ArrayList<Long>();
		for (BatchTiersHistoEntry e : batch.entries) {
			if (e.tiers == null) {
				ids.add(null);
			}
			else {
				ids.add(e.number);
			}
		}
		Assert.isTrue(ids.size() == size);

		final List<Niveau> niveaux = SecurityProvider.getDroitsAcces(ids);
		Assert.isTrue(niveaux.size() == size);

		for (int i = 0; i < ids.size(); ++i) {
			final BatchTiersHistoEntry entry = batch.entries.get(i);
			if (entry.tiers == null) {
				continue;
			}
			final Niveau niveau = niveaux.get(i);
			if (niveau == null) {
				String message = "L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + "/"
						+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + entry.number;
				entry.tiers = null;
				entry.exceptionMessage = message;
				entry.exceptionType = WebServiceExceptionType.ACCESS_DENIED;
			}
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture et écriture sur le tiers spécifié.
	 *
	 * @param tiersId le tiers sur lequel on veut vérifier les droits d'accès
	 * @throws AccessDeniedException si l'utilisateur courant ne possède pas les droits de lecture et écriture
	 */
	private static void checkTiersWriteAccess(long tiersId) throws AccessDeniedException {
		final Niveau acces = SecurityProvider.getDroitAcces(tiersId);
		if (acces == null || acces == Niveau.LECTURE) {
			throw new AccessDeniedException("L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + "/"
					+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en écriture sur le tiers n° " + tiersId);
		}
	}

	/**
	 * Vérifie que l'id du tiers retourné corresponds bien à celui demandé.
	 *
	 * @param expected l'id demandé
	 * @param actual   l'id retourné
	 * @throws ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException
	 *          si les deux ids ne sont pas égaux.
	 */
	private void assertCoherence(long expected, long actual) throws TechnicalException {
		if (expected != actual) {
			throw new TechnicalException(String.format(
					"Incohérence des données retournées détectées: tiers demandé = %d, tiers retourné = %d.", expected, actual));
		}
	}

	/**
	 * Vérifie que l'id de chaque tiers retourné corresponds bien à celui demandé.
	 *
	 * @param batch le batch à vérifier
	 * @throws ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException
	 *          si les ids retournés ne correspondent pas à ceux demandés.
	 */
	private void checkBatchCoherence(BatchTiers batch) throws TechnicalException {
		for (BatchTiersEntry e : batch.entries) {
			if (e.tiers != null) {
				assertCoherence(e.number, e.tiers.numero);
			}
		}
	}

	/**
	 * Vérifie que l'id de chaque tiers retourné corresponds bien à celui demandé.
	 *
	 * @param batch le batch à vérifier
	 * @throws ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException
	 *          si les ids retournés ne correspondent pas à ceux demandés.
	 */
	private void checkBatchCoherence(BatchTiersHisto batch) throws TechnicalException {
		for (BatchTiersHistoEntry e : batch.entries) {
			if (e.tiers != null) {
				assertCoherence(e.number, e.tiers.numero);
			}
		}
	}

	/**
	 * Log en erreur les exceptions embeddées dans le batch spécifié.
	 *
	 * @param params le message initial
	 * @param batch  les données retournées
	 */
	private void logEmbeddedExceptions(GetBatchTiers params, BatchTiers batch) {

		List<BatchTiersEntry> inError = null;

		for (BatchTiersEntry entry : batch.entries) {
			if (entry.exceptionMessage != null) {
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
				message.append("\n - id=").append(entry.number);
				message.append(", exception=\"").append(entry.exceptionMessage);
				message.append("\", type=").append(entry.exceptionType);
			}
			LOGGER.error(message.toString());
		}
	}

	/**
	 * Log en erreur les exceptions embeddées dans le batch spécifié.
	 *
	 * @param params le message initial
	 * @param batch  les données retournées
	 */
	private void logEmbeddedExceptions(GetBatchTiersHisto params, BatchTiersHisto batch) {

		List<BatchTiersHistoEntry> inError = null;

		for (BatchTiersHistoEntry entry : batch.entries) {
			if (entry.exceptionMessage != null) {
				if (inError == null) {
					inError = new ArrayList<BatchTiersHistoEntry>();
				}
				inError.add(entry);
			}
		}

		if (inError != null) {
			final StringBuilder message = new StringBuilder();
			message.append("Les exceptions suivantes ont été levées lors du traitement du message ").append(params).append(" : ");
			for (BatchTiersHistoEntry entry : inError) {
				message.append("\n - id=").append(entry.number);
				message.append(", exception=\"").append(entry.exceptionMessage);
				message.append("\", type=").append(entry.exceptionType);
			}
			LOGGER.error(message.toString());
		}
	}

	/**
	 * Log en erreur les erreurs rencontrées dans les demandes de quittancement
	 * @param params le message de demande de quittancements
	 * @param reponses les données retournées
	 */
	private void logEmbeddedErrors(QuittancerDeclarations params, List<ReponseQuittancementDeclaration> reponses) {

		// 1. collection des cas en erreur
		List<ReponseQuittancementDeclaration> inError = null;
		for (ReponseQuittancementDeclaration reponse : reponses) {
			if (reponse.code != CodeQuittancement.OK) {
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
				b.append("\n - key=").append(reponse.key);
				b.append(", code=").append(reponse.code);
				if (reponse.code == CodeQuittancement.EXCEPTION) {
					b.append(", exception=\"").append(reponse.exceptionMessage).append("\", type=").append(reponse.exceptionType);
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
