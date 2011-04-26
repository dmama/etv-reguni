package ch.vd.uniregctb.common;

import java.util.Stack;

import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.security.UniregSecurityDetails;

public class AuthenticationHelper {

	public static final String SYSTEM_USER = "[system]";
	private static final Authentication NULL_AUTH = createAuthentication("__null__");

	private static final ThreadLocal<Stack<Authentication>> stackByThread = new ThreadLocal<Stack<Authentication>>();

	private static Stack<Authentication> stack() {
		Stack<Authentication> stack = stackByThread.get();
		if (stack == null) {
			stack = new Stack<Authentication>();
			stackByThread.set(stack);
		}
		return stack;
	}

	public static void setPrincipal(String username) {

		UsernamePasswordAuthenticationToken authentication = createAuthentication(username);

		/* Enregistre le context de sécurité */
		setAuthentication(authentication);
	}

	/**
	 * crée un objet Authentication
	 *
	 * @param username un nom de l'utilisateur
	 * @return un objet Authentication
	 */
	private static UsernamePasswordAuthenticationToken createAuthentication(String username) {

		GrantedAuthority auth = new GrantedAuthorityImpl(username);
		GrantedAuthority[] authorities = new GrantedAuthority[]{
				auth
		};
		User user = new User(username, "noPwd", true, true, true, true, authorities);
		return new UsernamePasswordAuthenticationToken(user, "noPwd");
	}

	/**
	 * Défini un nouvel utilisateur principal et mémorise-là de manière à la récupérer avec {@link #popPrincipal()}.
	 *
	 * @param username
	 *            le nom de l'utilsateur principal
	 */
	public static void pushPrincipal(String username) {
		final Authentication current;
		if (getAuthentication() == null) {
			// pas autentifier -> on stock une authentification nulle de manière à retrouver la fin de la pile.
			current = NULL_AUTH;
		}
		else {
			current = getAuthentication();
			Assert.notNull(current);
		}
		stack().push(current);

		// crée et enregistre le nouveau context de sécurité */
		setAuthentication(createAuthentication(username));
	}

	/**
	 * Récupère l'utilisateur précédent et défini-le comme l'utilisateur principal.
	 * <p>
	 * <b>Note:</b> un utilisateur doit avoir été mémorisé précédemment avec {@link #pushPrincipal(String)}.
	 */
	public static void popPrincipal() {
		final Authentication previous = stack().pop();
		if (previous == NULL_AUTH){
			resetAuthentication();
		}
		else {
			setAuthentication(previous);
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
