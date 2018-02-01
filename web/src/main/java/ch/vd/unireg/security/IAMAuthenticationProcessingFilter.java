package ch.vd.unireg.security;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.filter.GenericFilterBean;

import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.EncodingFixHelper;

/**
 * Ce filtre permet de récupérer les informations d'autentification renseignées par IAM : le visa, prénom/nom et les rôles. Il vérifie ensuite que l'utilisateur possède bien un des rôles nécessaires
 * pour consulter l'application.
 */
public class IAMAuthenticationProcessingFilter extends GenericFilterBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(IAMAuthenticationProcessingFilter.class);

	private static final String visaHeaderKey = "iam-userid";
	private static final String firstnameHeaderKey = "iam-firstname";
	private static final String lastnameHeaderKey = "iam-lastname";
	private static final String applicationHeaderKey = "iam-application";
	private static final String rolesHeaderKey = "iam-roles";
	private static final String ACEGI_SECURITY_FORM_USERNAME_KEY = "j_username";

	private Class<? extends IAMDetails> detailsClass;
	private Set<String> allowedRoles;

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {

		if (AuthenticationHelper.getAuthentication() == null) {

			final HttpServletRequest request = (HttpServletRequest) servletRequest;

			// Récupération des infos de base

			final String visa = getVisa(request);
			if (StringUtils.isBlank(visa)) {
				throw new UsernameNotFoundException("Le visa de l'utilisateur n'est pas renseigné dans la requête.");
			}

			final String firstName = getHeaderString(request, firstnameHeaderKey);
			final String lastName = getHeaderString(request, lastnameHeaderKey);
			final String[] roles = getRoles(request);

			// Vérification des rôles

			checkRoles(visa, roles);

			// On peut maintenant enregistrer le context de sécurité

			final IAMDetails details;
			try {
				details = detailsClass.newInstance();
			}
			catch (Exception e) {
				throw new RuntimeException(e);
			}
			details.setIamFirstName(firstName);
			details.setIamLastName(lastName);
			details.setIamRoles(roles);

			LOGGER.info(String.format("Ouverture de la session pour l'utilisateur %s %s (%s)", firstName, lastName, visa));

			final List<GrantedAuthority> granted = Collections.singletonList(new SimpleGrantedAuthority(visa));
			final User user = new User(visa, "noPwd", true, true, true, true, granted);
			final UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, "noPwd", granted);
			authentication.setDetails(details);

			AuthenticationHelper.setAuthentication(authentication);
		}

		filterChain.doFilter(servletRequest, servletResponse);
	}

	private void checkRoles(String visa, String[] roles) {
		
		boolean noRoleGranted = true;

		for (String role : roles) {
			if (role != null && allowedRoles.contains(role)) {
				noRoleGranted = false;
				break;
			}
		}

		if (noRoleGranted) {
			throw new AuthenticationFailedException(
					"L'utilisateur " + visa + " ne possède aucun des rôles valides [" + Arrays.toString(allowedRoles.toArray()) + "] pour utiliser l'application.");
		}
	}

	private static String[] getRoles(HttpServletRequest request) {
		final String application = getHeaderString(request, applicationHeaderKey);
		final String allRoles = getHeaderString(request, rolesHeaderKey);
		return IAMUtil.createRoles(application, allRoles);
	}

	private static String getVisa(HttpServletRequest request) {
		String username = getHeaderString(request, visaHeaderKey);
		if (StringUtils.isBlank(username)) {
			username = decodeHeaderString(request.getParameter(ACEGI_SECURITY_FORM_USERNAME_KEY));
		}
		return username;
	}

	/**
	 * Retourne un paramètre stockée dans le header http de la requête. Cette méthode s'assure que l'encoding de la chaîne de caractères retournée est correct.
	 *
	 * @param request   la requête http
	 * @param key le nom du paramètre
	 * @return la valeur string du paramètre
	 */
	private static String getHeaderString(HttpServletRequest request, String key) {
		return decodeHeaderString(request.getHeader(key));
	}

	/**
	 * [SIFISC-2424] Il semble que toutes les strings stockés dans le header http soient encodées en ISO-8859-1, on effectue donc la conversion à la main.
	 *
	 * @param string une string directement récupérée du header http
	 * @return la même string décodée en UTF-8.
	 */
	private static String decodeHeaderString(String string) {
		return EncodingFixHelper.fixFromIso(string);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDetailsClass(Class<? extends IAMDetails> detailsClass) {
		this.detailsClass = detailsClass;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setAllowedRoles(Set<String> allowedRoles) {
		this.allowedRoles = allowedRoles;
	}
}
