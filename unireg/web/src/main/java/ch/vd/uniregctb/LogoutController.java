package ch.vd.uniregctb;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.security.UniregSecurityDetails;
import ch.vd.uniregctb.utils.UniregProperties;

/**
 * Contrôleur de logout. Invalide simplement la session. Ne retourne pas de vue pour éviter que l'appel à une JSP ne provoque
 * l'ouverture d'une autre session vide.
 */
@Controller
public class LogoutController {

	private static final Logger logger = LoggerFactory.getLogger(LogoutController.class);

	private UniregProperties uniregProperties;

	@RequestMapping(value = "/logoutIAM.do")
	public String logout(HttpServletRequest request, HttpServletResponse response) throws IOException {

		final AbstractAuthenticationToken auth = AuthenticationHelper.getAuthentication();
		final UniregSecurityDetails details = (UniregSecurityDetails) (auth == null ? null : auth.getDetails());
		if (details != null) {
			logger.info(String.format("Fermeture de la session pour l'utilisateur %s %s", details.getIamFirstName(), details.getIamLastName()));
		}

		SecurityContextHolder.clearContext();

		final HttpSession session = request.getSession(false);
		if (session != null) {
			session.invalidate();
		}

		final String host = request.getHeader("host");
		String url = uniregProperties.getProperty("iam.logout.url");
		if (StringUtils.isNotBlank(url) && StringUtils.isNotBlank(host)) {
			url = url.replace("{HOST}", host);
		}
		response.sendRedirect(url);
		return null;
	}

	public void setUniregProperties(UniregProperties uniregProperties) {
		this.uniregProperties = uniregProperties;
	}
}
