package ch.vd.uniregctb.webservices.tiers2.impl;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.tiers2.TiersWebService;
import ch.vd.uniregctb.webservices.tiers2.data.*;
import ch.vd.uniregctb.webservices.tiers2.data.Tiers.Type;
import ch.vd.uniregctb.webservices.tiers2.exception.AccessDeniedException;
import ch.vd.uniregctb.webservices.tiers2.exception.BusinessException;
import ch.vd.uniregctb.webservices.tiers2.exception.TechnicalException;
import ch.vd.uniregctb.webservices.tiers2.exception.WebServiceExceptionType;
import ch.vd.uniregctb.webservices.tiers2.params.*;
import org.apache.log4j.Logger;
import org.springframework.util.Assert;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.util.ArrayList;
import java.util.List;

/**
 * Cette classe réceptionne tous les appels au web-service, authentifie l'utilisateur, vérifie ses droits d'accès et finalement redirige les
 * appels vers l'implémentation concrète du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", name = "TiersPort", serviceName = "TiersService")
public class TiersWebServiceEndPoint implements TiersWebService {

	private static final Logger LOGGER = Logger.getLogger(TiersWebServiceEndPoint.class);

	private TiersWebService service;

	public void setService(TiersWebService service) {
		this.service = service;
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
		}
	}

	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public BatchTiers getBatchTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetBatchTiers") GetBatchTiers params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			final BatchTiers batch = service.getBatchTiers(params);
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
		}
	}

	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2")
	public BatchTiersHisto getBatchTiersHisto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers2", partName = "params", name = "GetBatchTiersHisto") GetBatchTiersHisto params)
			throws BusinessException, AccessDeniedException, TechnicalException {
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			final BatchTiersHisto batch = service.getBatchTiersHisto(params);
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
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			// TODO (msi) implémenter le contrôle d'accès au niveau PM
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
		if (acces == null || acces.equals(Niveau.LECTURE)) {
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
			StringBuilder message = new StringBuilder();
			message.append("Les exceptions suivantes ont été levées lors du traitement du message ").append(params).append(" : ");
			for (BatchTiersHistoEntry entry : inError) {
				message.append("\n - id=").append(entry.number);
				message.append(", exception=\"").append(entry.exceptionMessage);
				message.append("\", type=").append(entry.exceptionType);
			}
			LOGGER.error(message.toString());
		}
	}
}
