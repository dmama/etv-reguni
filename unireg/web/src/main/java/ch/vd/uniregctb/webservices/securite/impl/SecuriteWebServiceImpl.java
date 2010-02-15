package ch.vd.uniregctb.webservices.securite.impl;

import java.util.Set;

import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;
import javax.jws.soap.SOAPBinding;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.security.SecurityProvider;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceException;
import ch.vd.uniregctb.webservices.securite.GetAutorisationSurDossier;
import ch.vd.uniregctb.webservices.securite.GetDossiersControles;
import ch.vd.uniregctb.webservices.securite.NiveauAutorisation;
import ch.vd.uniregctb.webservices.securite.SecuriteWebService;

@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security", name = "SecuritePort", serviceName = "SecuriteService")
public class SecuriteWebServiceImpl implements SecuriteWebService {

	private static final Logger LOGGER = Logger.getLogger(SecuriteWebServiceImpl.class);

	private DroitAccesDAO dao;

	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security")
	public NiveauAutorisation getAutorisationSurDossier(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security", partName = "params", name = "GetAutorisationSurDossier") GetAutorisationSurDossier params)
			throws WebServiceException {

		try {
			login(params.login);
			final Niveau niveau = SecurityProvider.getDroitAcces(params.login.userId, params.numeroTiers);
			return EnumHelper.coreToWeb(niveau);
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

	@Deprecated
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security")
	public Set<Long> getDossiersControles(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security", partName = "params", name = "GetDossiersControles") GetDossiersControles params)
			throws WebServiceException {
		if (!params.authenticationToken.equals("I swear I am Host-Interface")) {
			// C'est pas host-interface, méchant méchant !
			throw new WebServiceException("All your bases are belongs to us !");
		}
		return dao.getContribuablesControles();
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

	public void setDao(DroitAccesDAO dao) {
		this.dao = dao;
	}
}
