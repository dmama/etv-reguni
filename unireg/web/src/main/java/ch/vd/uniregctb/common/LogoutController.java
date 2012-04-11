package ch.vd.uniregctb.common;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import ch.vd.uniregctb.security.UniregSecurityDetails;
import ch.vd.uniregctb.utils.UniregProperties;

/**
 * Controlleur de logout. Invalide simplement la sessions. Ne retourne pas de vue pour eviter que l'appel � une JSP par weblogic ne provoque
 * l'ouverture d'une autre session vide.
 *
 * @author <a href="mailto:abenaissi@cross-systems.com">Akram BEN AISSI </a>
 */
public class LogoutController implements Controller {
	private static final Logger logger = Logger.getLogger(LogoutController.class);

	private UniregProperties uniregProperties;

	/**
	 * @see org.springframework.web.servlet.mvc.Controller#handleRequest(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

		final AbstractAuthenticationToken auth = AuthenticationHelper.getAuthentication();
		final UniregSecurityDetails details = (UniregSecurityDetails) (auth == null ? null : auth.getDetails());
		if (details != null) {
			logger.info(String.format("Fermeture de la session pour l'utilisateur %s %s", details.getIamFirstName(), details.getIamLastName()));
		}

		SecurityContextHolder.clearContext();

		HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		final String host = request.getHeader("host");
		String url = uniregProperties.getProperty("iam.logout.url");
		if (!StringUtils.isBlank(url) && !StringUtils.isBlank(host)) {
			url = url.replace("{HOST}", host);
		}
		response.sendRedirect(url);
		return null;
	}

	/**
	 * @param uniregProperties the uniregProperties to set
	 */
	public void setUniregProperties(UniregProperties uniregProperties) {
		this.uniregProperties = uniregProperties;
	}
}
