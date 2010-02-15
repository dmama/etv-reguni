package ch.vd.uniregctb.common;

import java.util.Stack;

import org.acegisecurity.Authentication;
import org.acegisecurity.GrantedAuthority;
import org.acegisecurity.GrantedAuthorityImpl;
import org.acegisecurity.acls.sid.PrincipalSid;
import org.acegisecurity.context.SecurityContextHolder;
import org.acegisecurity.providers.AbstractAuthenticationToken;
import org.acegisecurity.providers.UsernamePasswordAuthenticationToken;
import org.acegisecurity.userdetails.User;

import ch.vd.uniregctb.security.UniregSecurityDetails;

public class AuthenticationHelper {

	public static final String SYSTEM_USER = "[system]";
	private static final String NULL_AUTH = "__null__";

	private static final ThreadLocal<Stack<String>> stackByThread = new ThreadLocal<Stack<String>>();

	private static Stack<String> stack() {
		Stack<String> stack = stackByThread.get();
		if (stack == null) {
			stack = new Stack<String>();
			stackByThread.set(stack);
		}
		return stack;
	}

	public static void setPrincipal(String username) {

		/* crée un objet Authentication */
		GrantedAuthority auth = new GrantedAuthorityImpl(username);
		GrantedAuthority[] authorities = new GrantedAuthority[] {
			auth
		};
		User user = new User(username, "noPwd", true, true, true, true, authorities);
		UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, "noPwd");

		/* Enregistre le context dans Acegi */
		setAuthentication(authentication);
	}

	/**
	 * Défini un nouvel utilisateur principal et mémorise-là de manière à la récupérer avec {@link #popPrincipal()}.
	 *
	 * @param username
	 *            le nom de l'utilsateur principal
	 */
	public static void pushPrincipal(String username) {
		final String current;
		if (getAuthentication() == null) {
			// pas autentifier -> on stock une authentification nulle de manière à retrouver la fin de la pile.
			current = NULL_AUTH;
		}
		else {
			current = getCurrentPrincipal();
		}
		stack().push(current);
		setPrincipal(username);
	}

	/**
	 * Récupère l'utilisateur précédent et défini-le comme l'utilisateur principal.
	 * <p>
	 * <b>Note:</b> un utilisateur doit avoir été mémorisé précédemment avec {@link #pushPrincipal(String)}.
	 */
	public static void popPrincipal() {
		final String previous = stack().pop();
		if (previous.equals(NULL_AUTH)) {
			resetAuthentication();
		}
		else {
			setPrincipal(previous);
		}
	}

	public static void setAuthentication(Authentication authentication) {
		SecurityContextHolder.getContext().setAuthentication(authentication);
	}

	public static void resetAuthentication() {
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	public static AbstractAuthenticationToken getAuthentication() {
		return (AbstractAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
	}

	/**
	 * @return whether or not someone is authentified for the current thread.
	 */
	public static boolean isAuthenticated() {
		return getAuthentication() != null;
	}

	public static String getCurrentPrincipal() {

		Authentication auth = getAuthentication();
		if (auth == null) {
			throw new IllegalArgumentException("L'authentification ne peut pas être nulle");
		}
		PrincipalSid sid = new PrincipalSid(auth);
		String principal = sid.getPrincipal();

		// LOGGER.info("Principal: "+principal);
		return principal;
	}

	private static UniregSecurityDetails getDetails() {
		AbstractAuthenticationToken auth = getAuthentication();
		if (auth == null) {
			throw new IllegalArgumentException("L'authentification ne peut pas être nulle");
		}
		UniregSecurityDetails d = (UniregSecurityDetails) auth.getDetails();
		if (d == null) {
			d = new UniregSecurityDetails();
			auth.setDetails(d);
		}
		return d;
	}

	public static Integer getCurrentOID() {
		Integer oid = null;
		UniregSecurityDetails details = getDetails();
		oid = details.getIfoSecOID();
		return oid;
	}

	public static String getCurrentOIDSigle() {
		UniregSecurityDetails details = getDetails();
		String sigle = details.getIfoSecOIDSigle();
		return sigle;
	}

	// Testing
	public static void setCurrentOID(int i) {
		UniregSecurityDetails details = getDetails();
		details.setIfoSecOID(i);
	}

}
