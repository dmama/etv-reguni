package ch.vd.uniregctb.security;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.acegisecurity.Authentication;
import org.acegisecurity.AuthenticationException;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.userdetails.UsernameNotFoundException;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Logger;

import ch.vd.ati.security.IAMAuthenticationProcessingFilter;
import ch.vd.ati.security.IAMUtil;

/**
 * Délégation d'authentification dans Acegi aprés l'authentification en amont
 * effectuée dans IAM.
 * <p>
 * Implémentation: Simplification de
 * org.acegisecurity.ui.webapp.SiteminderAuthenticationProcessingFilter avec
 * gestion en plus des rôles et application dans l'entête HTTP. </li>
 * 'iam-userid' est utilisé comme 'username' par Acegi </li>
 * 'iam-roles' doit contenir les rôles provenant de IAM. Voir aussi
 * 'setDetails()'.
 */
public class UniregIAMAuthProcessingFilter extends IAMAuthenticationProcessingFilter {

	private final static Logger LOGGER = Logger.getLogger(UniregIAMAuthProcessingFilter.class);

	public static final String UNIREG_IAM_FIRST = "UNIREG_IAM_FIRST";
	public static final String UNIREG_IAM_LAST = "UNIREG_IAM_LAST";

	/**
	 * La cle de username du header.
	 */
	private static final String usernameHeaderKey = "iam-userid";

	/**
	 * La cle application du header.
	 */
	private static final String applicationHeaderKey = "iam-application";

	/**
	 * La cle firstName du header.
	 */
	private static final String firstnameHeaderKey = "iam-firstname";

	/**
	 * La cle firstName du header.
	 */
	private static final String lastnameHeaderKey = "iam-lastname";

	/**
	 * La clé des roles du header. Par défaut 'iam-roles'.
	 */
	private static final String rolesHeaderKey = "iam-roles";

	/**
	 * Clé interne pour les roles dans le header. L'utilisateur final pourra
	 * ainsi la valeur par défaut de la clé des roles.
	 */
	public static final String _INTERNAL_ROLES_HEADER_KEY = "_internal-iam-roles-key";

	private static final String ACEGI_SECURITY_FORM_ROLES_KEY = "j_roles";

	public UniregIAMAuthProcessingFilter() {
		// hack pour que la super class utilise son propre logger
		super.logger = LogFactory.getLog(IAMAuthenticationProcessingFilter.class);
	}

	/**
	 * @see org.acegisecurity.ui.AbstractProcessingFilter#attemptAuthentication(javax.servlet.http.HttpServletRequest)
	 */
	@Override
	public Authentication attemptAuthentication(HttpServletRequest request) throws AuthenticationException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("AttemptAuthentication (debug=" + isDebug() + ") ...");
		}

		String username = null;
		String password = "";
		// Récupére le username dans le header HTTP.
		if (usernameHeaderKey.length() > 0) {
			username = request.getHeader(usernameHeaderKey);
		}

		// Si le bypass de l'autentification est autorisé, on recherche le
		// username dans le fichier de configuration.
		if (username == null && isDebug()) {
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Recherche du username dans le fichier de properties de l'application.");
			}
			username = SecurityDebugConfig.getIamBypassUser();
			// Si l'utilisateur de bypass n'est pas dans le fichier de conf, on
			// le recupere depuis l'URL.
			if ((username == null) || (username.length() == 0)) {
				username = request.getParameter(ACEGI_SECURITY_FORM_USERNAME_KEY);
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("Récupération de l'utilsateur depuis la request: username=" + username);
				}
			}
		}
		// Do not perform authentication for null username:
		if ((username == null) || "".equals(username)) {
			LOGGER.error("Le Username IAM est null");
			throw new UsernameNotFoundException("Le Username IAM est null");
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("IAM Username=" + username);
		}
		// Create authentication token:
		UsernamePasswordAuthenticationToken authRequest = new UsernamePasswordAuthenticationToken(username, password);
		// Allow subclasses to set the "details" property
		setDetails(request, authRequest);

		// Place the last username attempted into HttpSession for views
		request.getSession().setAttribute(ACEGI_SECURITY_LAST_USERNAME_KEY, username);
		// Delegate authentication and find roles:
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("attemptAuthentication...done");
		}
		return this.getAuthenticationManager().authenticate(authRequest);
	}

	/**
	 * Redefined. A Hashtable with the following entry is put as Details:
	 * <ul>
	 * <li> key = _INTERNAL_ROLES_HEADER_KEY </li>
	 * <li> value = String[], which contains the roles retrieved from the
	 * upfront system (i.e. IAM) for the current application. </li>
	 * </ul>
	 * The roles header key would contain all of the roles of the user. Split
	 * these roles and only take those that are relevant to this application.
	 *
	 * @see ch.vd.ati.security.IAMUtil#createsRoles
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void setDetails(HttpServletRequest request, UsernamePasswordAuthenticationToken authRequest) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("setDetails...");
		}

		if (LOGGER.isTraceEnabled()) {
			Enumeration<String> headerNames = request.getHeaderNames();
			while (headerNames.hasMoreElements()) {
				String headerName = headerNames.nextElement();
				LOGGER.trace(headerName + "=" + getHeaderString(request, headerName));
			}
		}

		// Retrieve application and roles from header keys:
		String application = getHeaderString(request, applicationHeaderKey);
		if (application == null && isDebug()) {
			application = SecurityDebugConfig.getIamBypassApplication();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Récupération de l'application depuis le fichier de properties : application=" + application);
			}
		}

		// First
		String firstName = getHeaderString(request, firstnameHeaderKey);
		if (firstName == null && isDebug()) {
			firstName = SecurityDebugConfig.getIamBypassFirstName();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Récupération du firstName depuis le fichier de properties : first=" + firstName);
			}
		}
		request.getSession().setAttribute(UNIREG_IAM_FIRST, firstName);
		// Last
		String lastName = getHeaderString(request, lastnameHeaderKey);
		if (lastName == null && isDebug()) {
			lastName = SecurityDebugConfig.getIamBypassLastName();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Récupération du lastName depuis le fichier de properties : last=" + lastName);
			}
		}
		request.getSession().setAttribute(UNIREG_IAM_LAST, lastName);
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("IAM first='"+firstName+"' last='"+lastName+"'");
		}

		String allRoles = getHeaderString(request, rolesHeaderKey);
		if (allRoles == null && isDebug()) {
			allRoles = SecurityDebugConfig.getIamBypassRoles();
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Récupération des roles depuis le fichier de properties de l'application : roles=" + allRoles);
			}
		}

		// Extract business roles:
		String[] roles = IAMUtil.createRoles(application, allRoles);
		if ((roles == null || roles.length == 0) && isDebug()) {
			String paramRoles = request.getParameter(ACEGI_SECURITY_FORM_ROLES_KEY);
			if (LOGGER.isTraceEnabled()) {
				LOGGER.trace("Récupération des roles depuis l'URL: roles=" + paramRoles);
			}
			if (paramRoles != null) {
				roles = paramRoles.split(",");
			}
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace(String.format("IAM user: %s %s. Application: %s. Roles: %s.", firstName, lastName, application, ArrayUtils.toString(roles)));
		}

		boolean rolesOk = false;
		for (String role : roles) {
			if (role.equalsIgnoreCase("user") || role.equalsIgnoreCase("user_externe")) {
				rolesOk = true;
				break;
			}
		}
		if (!rolesOk && !SecurityDebugConfig.isIamDebug()) {
			// voir la configuration du bean 'filterInvocationInterceptor' dans unireg-web-security.xml
			LOGGER.warn(String.format("L'utilisateur %s %s ne possède pas le rôle IAM 'user', il ne pourra pas utiliser l'application (roles = %s).",
					firstName, lastName, Arrays.toString(roles)));
		}

		if (request.getSession().getAttribute(IFOSecAuthenticationProcessingFilter.USER_OID_SIGLE) == null) { // si l'OID n'est pas stocké, c'est qu'il s'agit d'une nouvelle session
			LOGGER.info(String.format("Ouverture de la session pour l'utilisateur %s %s", firstName, lastName));
		}

		// Put business roles in Hashtable and put it in Details:
		UniregSecurityDetails details = new UniregSecurityDetails();
		details.setToAuthentication(authRequest);

		// Roles
		details.setIamRoles(roles);
		details.put(_INTERNAL_ROLES_HEADER_KEY, roles); // Nécessaire pour la sous-classe IAMAuthenticationProcessingFilter
		details.setIamFirstName(firstName);
		details.setIamLastName(lastName);
	}

	/**
	 * Retourne un paramètre stockée dans le header http de la requête. Cette méthode s'assure que l'encoding de la chaîne de caractères retournée est correct.
	 *
	 * @param request   la requête http
	 * @param parameter le nom du paramètre
	 * @return la valeur string du paramètre
	 */
	private String getHeaderString(HttpServletRequest request, String parameter) {
		return decodeHeaderString(request.getHeader(parameter));
	}

	/**
	 * [UNIREG-2335] Il semble que toutes les strings stockés dans le header http soient encodées en ISO-8859-1, on effectue donc la conversion à la main.
	 *
	 * @param string une string directement récupérée du header http
	 * @return la même string décodée en UTF-8.
	 */
	private static String decodeHeaderString(String string) {
		if (string != null) {
			try {
				final byte[] bytes = string.getBytes("ISO-8859-1");
				string = new String(bytes, "UTF-8");
			}
			catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
		}
		return string;
	}

	@Override
	protected boolean requiresAuthentication(final HttpServletRequest request, final HttpServletResponse response) {
    	// Hack pour le debugging.
		return SecurityDebugConfig.isReloadEachTime() || super.requiresAuthentication(request, response);
	}

	private boolean isDebug() {
		return SecurityDebugConfig.isIamDebug();
	}
}
