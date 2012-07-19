package ch.vd.uniregctb.common;

import java.util.Arrays;
import java.util.Stack;

import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.GrantedAuthorityImpl;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.security.UniregSecurityDetails;

public class AuthenticationHelper {

	public static final String SYSTEM_USER = "[system]";
	private static final Authentication NULL_AUTH = createAuthentication("__null__");

	private static final ThreadLocal<Stack<Authentication>> stackByThread = new ThreadLocal<Stack<Authentication>>() {
		@Override
		protected Stack<Authentication> initialValue() {
			return new Stack<Authentication>();
		}
	};

	private static Stack<Authentication> stack() {
		return stackByThread.get();
	}

	/**
	 * crée un objet Authentication
	 *
	 * @param username un nom de l'utilisateur
	 * @return un objet Authentication
	 */
	private static UsernamePasswordAuthenticationToken createAuthentication(String username) {
		final User user = new User(username, "noPwd", true, true, true, true, Arrays.asList(new GrantedAuthorityImpl(username)));
		return new UsernamePasswordAuthenticationToken(user, "noPwd");
	}

	/**
	 * crée un objet Authentication
	 *
	 * @param username un nom de l'utilisateur
	 * @return un objet Authentication
	 */
	private static UsernamePasswordAuthenticationToken createAuthentication(String username, int oid) {
		final UsernamePasswordAuthenticationToken authentication = createAuthentication(username);
		final UniregSecurityDetails details = getDetails(authentication);
		details.setIfoSecOID(oid);
		return authentication;
	}

	/**
	 * Définit un nouvel utilisateur principal et mémorise-là de manière à la récupérer avec {@link #popPrincipal()}.
	 *
	 * @param username le visa de l'utilisateur principal
	 */
	public static void pushPrincipal(String username) {
		// crée et enregistre le nouveau context de sécurité */
		pushAuthentication(createAuthentication(username));
	}

	/**
	 * Définit un nouvel utilisateur principal et mémorise-là de manière à la récupérer avec {@link #popPrincipal()}.
	 *
	 * @param username le visa de l'utilisateur principal
	 * @param oid l'oid de l'utilisateur
	 */
	public static void pushPrincipal(String username, int oid) {
		// crée et enregistre le nouveau context de sécurité */
		pushAuthentication(createAuthentication(username, oid));
	}

	private static void pushAuthentication(Authentication authentication) {
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

		// enregistre le nouveau context de sécurité */
		setAuthentication(authentication);
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

	public static String getCurrentPrincipalOrSystem() {
		Authentication auth = getAuthentication();
		if (auth == null) {
			return SYSTEM_USER;
		}
		return getCurrentPrincipal();
	}

	private static UniregSecurityDetails getDetails() {
		AbstractAuthenticationToken auth = getAuthentication();
		if (auth == null) {
			throw new IllegalArgumentException("L'authentification ne peut pas être nulle");
		}
		return getDetails(auth);
	}

	private static UniregSecurityDetails getDetails(AbstractAuthenticationToken auth) {
		UniregSecurityDetails d = (UniregSecurityDetails) auth.getDetails();
		if (d == null) {
			d = new UniregSecurityDetails();
			auth.setDetails(d);
		}
		return d;
	}

	public static Integer getCurrentOID() {
		final UniregSecurityDetails details = getDetails();
		return details.getIfoSecOID();
	}

	public static String getCurrentOIDSigle() {
		final UniregSecurityDetails details = getDetails();
		return details.getIfoSecOIDSigle();
	}

	public static void setCurrentOID(int i, String sigle) {
		final UniregSecurityDetails details = getDetails();
		details.setIfoSecOID(i);
		details.setIfoSecOIDSigle(sigle);
	}

	public static void setCurrentOID(int i) {
		final UniregSecurityDetails details = getDetails();
		details.setIfoSecOID(i);
	}

	public static String getFirstName() {
		final UniregSecurityDetails details = getDetails();
		return details.getIamFirstName();
	}

	public static String getLastName() {
		final UniregSecurityDetails details = getDetails();
		return details.getIamLastName();
	}
}
