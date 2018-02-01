package ch.vd.unireg;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.security.UniregSecurityDetails;
import ch.vd.unireg.utils.UniregProperties;

/**
 * Contrôleur de logout. Invalide simplement la session. Ne retourne pas de vue pour éviter que l'appel à une JSP ne provoque l'ouverture d'une autre session vide.
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

		// [SIFISC-26633] on détermine le chemin relatif pour remonter sur le host
		final ServletContext servletContext = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest().getServletContext();
		final String logoutUrl = uniregProperties.getProperty("iam.logout.url");    // e.g. "https://{HOST}/iam/accueil/"
		final String path = servletContext.getContextPath();                        // e.g. "/fiscalite/int-unireg/web"
		final String relativeLogoutUrl = buildRelativeLogoutUrl(logoutUrl, path);   // e.g. "../../../iam/accueil/"

		// on effectue la redirection
		response.sendRedirect(relativeLogoutUrl);
		return null;
	}

	/**
	 * [SIFISC-26633] construit une url relative pour renvoyer vers la page de logout qui va bien.
	 *
	 * @param logoutUrl   l'url de logout absolue
	 * @param contextPath le context de la servlet
	 * @return une url relative
	 */
	@NotNull
	static String buildRelativeLogoutUrl(String logoutUrl, String contextPath) {
		final int subDirCount = StringUtils.countMatches(contextPath, "/");         // e.g. "/fiscalite/int-unireg/web" -> 3
		final String upToHost = StringUtils.repeat("../", subDirCount);             // e.g. "../../../"
		final String logoutPath = logoutUrl.replace("https://{HOST}/", "");         // e.g. "iam/accueil/"
		return upToHost + logoutPath;
	}

	public void setUniregProperties(UniregProperties uniregProperties) {
		this.uniregProperties = uniregProperties;
	}
}
