package ch.vd.uniregctb.webservices.securite.impl;

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
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.transport.http.AbstractHTTPDestination;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.load.DetailedLoadMeter;
import ch.vd.uniregctb.security.DroitAccesDAO;
import ch.vd.uniregctb.security.SecurityProviderInterface;
import ch.vd.uniregctb.stats.DetailedLoadMonitorable;
import ch.vd.uniregctb.stats.LoadDetail;
import ch.vd.uniregctb.type.Niveau;
import ch.vd.uniregctb.webservices.common.UserLogin;
import ch.vd.uniregctb.webservices.common.WebServiceException;
import ch.vd.uniregctb.webservices.securite.GetAutorisationSurDossier;
import ch.vd.uniregctb.webservices.securite.GetDossiersControles;
import ch.vd.uniregctb.webservices.securite.NiveauAutorisation;
import ch.vd.uniregctb.webservices.securite.SecuriteWebService;

@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
@WebService(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security", name = "SecuritePort", serviceName = "SecuriteService")
public class SecuriteWebServiceImpl implements SecuriteWebService, DetailedLoadMonitorable {

	private static final Logger LOGGER = LoggerFactory.getLogger(SecuriteWebServiceImpl.class);
	private static final Logger ACCESS_LOG = LoggerFactory.getLogger("securite.read");

	private DroitAccesDAO dao;
	private SecurityProviderInterface securityProvider;

	@Resource
	private WebServiceContext context;

	/**
	 * Moniteur des appels en cours
	 */
	private final DetailedLoadMeter<Object> loadMeter = new DetailedLoadMeter<>();

	@Override
	public int getLoad() {
		return loadMeter.getLoad();
	}

	@Override
	public List<LoadDetail> getLoadDetails() {
		return loadMeter.getLoadDetails();
	}

	@Override
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security")
	public NiveauAutorisation getAutorisationSurDossier(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security", partName = "params", name = "GetAutorisationSurDossier") GetAutorisationSurDossier params)
			throws WebServiceException {

		Throwable t = null;
		final Instant start = loadMeter.start(params);
		try {
			login(params.login);
			try {
				final Niveau niveau = securityProvider.getDroitAcces(params.login.userId, params.numeroTiers);
				return EnumHelper.coreToWeb(niveau);
			}
			finally {
				logout();
			}
		}
		catch (WebServiceException e) {
			LOGGER.error(e.getMessage(), e);
			t = e;
			throw e;
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			t = e;
			throw new WebServiceException(e);
		}
		finally {
			final Instant end = loadMeter.end();
			logAccess(params, Duration.between(start, end), t);
		}
	}

	@Override
	@Deprecated
	@SOAPBinding(parameterStyle = SOAPBinding.ParameterStyle.BARE)
	@WebMethod
	@WebResult(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security")
	@Transactional(readOnly = true)
	public Set<Long> getDossiersControles(
			@WebParam(targetNamespace = "http://www.vd.ch/uniregctb/webservices/security", partName = "params", name = "GetDossiersControles") GetDossiersControles params)
			throws WebServiceException {

		Throwable t = null;
		final Instant start = loadMeter.start(params);
		try {
			if (!params.authenticationToken.equals("I swear I am Host-Interface")) {
				// C'est pas host-interface, méchant méchant !
				throw new WebServiceException("All your bases are belongs to us !");
			}
			return dao.getContribuablesControles();
		}
		catch (WebServiceException | RuntimeException e) {
			t = e;
			throw e;
		}
		finally {
			final Instant end = loadMeter.end();
			logAccess(params, Duration.between(start, end), t);
		}
	}

	/**
	 * Login l'utilisateur dans l'application.
	 */
	private void login(UserLogin login) throws WebServiceException {
		if (login == null || login.userId == null || login.oid == null || login.userId.trim().isEmpty()) {
			throw new WebServiceException("L'identification de l'utilisateur (userId + oid) doit être renseignée.");
		}
		AuthenticationHelper.pushPrincipal(login.userId, login.oid);
	}

	/**
	 * Logout l'utilisateur de l'application
	 */
	private void logout() {
		AuthenticationHelper.resetAuthentication();
	}

	/**
	 * Log les paramètres et la durée d'un appel en read-only
	 *
	 * @param params   les paramètres de l'appel
	 * @param duration la durée de l'appel
	 * @param t l'éventuelle exception lancée dans l'appel
	 */
	private void logAccess(Object params, Duration duration, Throwable t) {
		if (ACCESS_LOG.isInfoEnabled()) {
			final String user = getBasicAuthenticationUser();
			final String exceptionString = (t == null ? StringUtils.EMPTY : String.format(", %s thrown", t.getClass()));

			// getLoad()+1 : +1 car le logout a déjà été fait quand on arrive ici et l'appel courant a donc été décompté
			ACCESS_LOG.info(String.format("[%s] (%d ms) %s load=%d%s", user, duration.toMillis(), params.toString(), loadMeter.getLoad() + 1, exceptionString));
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

	public void setDao(DroitAccesDAO dao) {
		this.dao = dao;
	}

	public void setSecurityProvider(SecurityProviderInterface securityProvider) {
		this.securityProvider = securityProvider;
	}
}
