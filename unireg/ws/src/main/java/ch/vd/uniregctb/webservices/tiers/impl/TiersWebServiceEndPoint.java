package ch.vd.uniregctb.webservices.tiers.impl;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;
import java.util.List;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.security.Role;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceException;
import ch.vd.uniregctb.webservices.tiers.Tiers;
import ch.vd.uniregctb.webservices.tiers.Tiers.Type;
import ch.vd.uniregctb.webservices.tiers.TiersHisto;
import ch.vd.uniregctb.webservices.tiers.TiersInfo;
import ch.vd.uniregctb.webservices.tiers.TiersWebService;
import ch.vd.uniregctb.webservices.tiers.params.AllConcreteTiersClasses;
import ch.vd.uniregctb.webservices.tiers.params.GetTiers;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersHisto;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersPeriode;
import ch.vd.uniregctb.webservices.tiers.params.GetTiersType;
import ch.vd.uniregctb.webservices.tiers.params.SearchTiers;
import ch.vd.uniregctb.webservices.tiers.params.SetTiersBlocRembAuto;

/**
 * Cette classe réceptionne tous les appels au web-service, authentifie l'utilisateur, vérifie ses droits d'accès et finalement redirige les
 * appels vers l'implémentation concrète du service.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", name = "TiersPort", serviceName = "TiersService")
public class TiersWebServiceEndPoint implements TiersWebService {

	private static final Logger LOGGER = Logger.getLogger(TiersWebServiceEndPoint.class);

	private TiersWebService service;

	public TiersWebService getService() {
		return service;
	}

	public void setService(TiersWebService service) {
		this.service = service;
	}

	/**
	 * {@inheritDoc}
	 */
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public List<TiersInfo> searchTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "SearchTiers") SearchTiers params)
			throws WebServiceException {
		try {
			login(params.login);
			checkLimitedReadAccess(params.login);
			return service.searchTiers(params);
		}
		catch (WebServiceException e) {
			LOGGER.error(e, e);
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
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
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public Tiers.Type getTiersType(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "GetTiersType") GetTiersType params)
			throws WebServiceException {
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			final Type type = service.getTiersType(params);
			if (type != null) {
				checkTiersReadAccess(params.tiersNumber);
			}
			return type;
		}
		catch (WebServiceException e) {
			LOGGER.error(e, e);
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
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
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public Tiers getTiers(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "GetTiers") GetTiers params)
			throws WebServiceException {
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
		catch (WebServiceException e) {
			LOGGER.error(e, e);
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
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
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public TiersHisto getTiersPeriode(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "GetTiersPeriode") GetTiersPeriode params)
			throws WebServiceException {
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
		catch (WebServiceException e) {
			LOGGER.error(e, e);
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
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
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public TiersHisto getTiersHisto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "GetTiersHisto") GetTiersHisto params)
			throws WebServiceException {
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
		catch (WebServiceException e) {
			LOGGER.error(e, e);
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
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
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public void setTiersBlocRembAuto(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers", partName = "params", name = "SetTiersBlocRembAuto") SetTiersBlocRembAuto params)
			throws WebServiceException {
		try {
			login(params.login);
			checkGeneralReadAccess(params.login);
			checkTiersWriteAccess(params.tiersNumber);
			service.setTiersBlocRembAuto(params);
		}
		catch (WebServiceException e) {
			LOGGER.error(e, e);
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new WebServiceException(e);
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
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/tiers")
	public void doNothing(AllConcreteTiersClasses dummy) {
	}

	/**
	 * Login l'utilisateur dans l'application.
	 */
	private void login(UserLogin login) throws WebServiceException {
		if (login == null || login.userId == null || login.oid == null || login.userId.trim().equals("")) {
			throw new WebServiceException("L'identification de l'utilisateur (userId + oid) doit être renseignée.");
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
	 * @param login
	 *            l'information de login de l'utilisareur
	 * @throws WebServiceException
	 *             si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkLimitedReadAccess(UserLogin login) throws WebServiceException {
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.userId, login.oid) &&
				!SecurityProvider.isGranted(Role.VISU_LIMITE, login.userId, login.oid)) {
			throw new WebServiceException("L'utilisateur spécifié (" + login.userId + "/" + login.oid
					+ ") n'a pas les droits d'accès en lecture sur l'application.");
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur l'application en général.
	 *
	 * @param login
	 *            l'information de login de l'utilisareur
	 * @throws WebServiceException
	 *             si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkGeneralReadAccess(UserLogin login) throws WebServiceException {
		if (!SecurityProvider.isGranted(Role.VISU_ALL, login.userId, login.oid)) {
			throw new WebServiceException("L'utilisateur spécifié (" + login.userId + "/" + login.oid
					+ ") n'a pas les droits d'accès en lecture complète sur l'application.");
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture sur le tiers spécifié.
	 *
	 * @param tiersId
	 *            le tiers sur lequel on veut vérifier les droits d'accès
	 * @throws WebServiceException
	 *             si l'utilisateur courant ne possède pas les droits de lecture
	 */
	private static void checkTiersReadAccess(long tiersId) throws WebServiceException {
		final Niveau acces = SecurityProvider.getDroitAcces(tiersId);
		if (acces == null) {
			throw new WebServiceException("L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + "/"
					+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en lecture sur le tiers n° " + tiersId);
		}
	}

	/**
	 * Vérifie que l'utilisateur courant possède bien les droits de lecture et écriture sur le tiers spécifié.
	 *
	 * @param tiersId
	 *            le tiers sur lequel on veut vérifier les droits d'accès
	 * @throws WebServiceException
	 *             si l'utilisateur courant ne possède pas les droits de lecture et écriture
	 */
	private static void checkTiersWriteAccess(long tiersId) throws WebServiceException {
		final Niveau acces = SecurityProvider.getDroitAcces(tiersId);
		if (acces == null || acces == Niveau.LECTURE) {
			throw new WebServiceException("L'utilisateur spécifié (" + AuthenticationHelper.getCurrentPrincipal() + "/"
					+ AuthenticationHelper.getCurrentOID() + ") n'a pas les droits d'accès en écriture sur le tiers n° " + tiersId);
		}
	}

	/**
	 * Vérifie que l'id du tiers retourné corresponds bien à celui demandé.
	 */
	private void assertCoherence(long expected, long actual) {
		if (expected != actual) {
			throw new IllegalArgumentException(String.format(
					"Incohérence des données retournées détectées: tiers demandé = %d, tiers retourné = %d.", expected, actual));
		}
	}
}
