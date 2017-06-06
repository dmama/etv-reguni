package ch.vd.unireg.servlet.security;

import javax.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;

/**
 * Classe utilitaire qui permet de centraliser le code de récupération de l'utilisateur connecté
 * (en particulier pour les web-services)
 */
@SuppressWarnings("unused")
public abstract class AuthenticatedUserHelper {

	private static final String UNKNOWN_USER = "n/a";
	private static final String IAM_USERNAME_HEADER = "iam-userid";

	private static final List<Function<HttpServletRequest, Optional<String>>> AUTHENTICATED_USER_EXTRACTORS = buildAuthenticatedUserExtractors();

	private static List<Function<HttpServletRequest, Optional<String>>> buildAuthenticatedUserExtractors() {
		final List<Function<HttpServletRequest, Optional<String>>> extractors = new ArrayList<>(2);

		extractors.add(AuthenticatedUserHelper::getUserPrincipal);                 // d'abord la basic authentication classique..
		extractors.add(AuthenticatedUserHelper::getIamAuthenticatedUser);          // et si cela n'a rien donné, on va voir dans l'attribut IAM

		return Collections.unmodifiableList(extractors);
	}

	/**
	 * @param request requête HTTP entrante
	 * @return la chaîne tirée du champ "UserPrincipal" de la requête (basic-auth)
	 */
	private static Optional<String> getUserPrincipal(HttpServletRequest request) {
		return Optional.ofNullable(request)
				.map(HttpServletRequest::getUserPrincipal)
				.map(Principal::getName);
	}

	/**
	 * @param request requête HTTP entrante
	 * @return la chaîne tirée du header IAM de la requête
	 */
	private static Optional<String> getIamAuthenticatedUser(HttpServletRequest request) {
		return Optional.ofNullable(request)
				.map(req -> req.getHeader(IAM_USERNAME_HEADER))
				.filter(StringUtils::isNotBlank);
	}

	/**
	 * @return le nom de l'utilisateur utilisé pour se connecter au web-service en mode <i>basic authentication</i>; ou "n/a" si cette information n'existe pas.
	 */
	public static String getAuthenticatedUser(HttpServletRequest request) {
		return AUTHENTICATED_USER_EXTRACTORS.stream()
				.map(extrator -> extrator.apply(request))
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst()
				.orElse(UNKNOWN_USER);
	}
}
