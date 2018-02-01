package ch.vd.uniregctb.common;

import java.util.ArrayDeque;
import java.util.Deque;

import org.jetbrains.annotations.NotNull;
import org.springframework.security.acls.domain.PrincipalSid;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import ch.vd.uniregctb.security.UniregSecurityDetails;

public abstract class AuthenticationHelper {

	public static final String SYSTEM_USER = "[system]";

	private static final String MISSING_AUTH = "Missing authentication information!";

	private static final class StackableData {
		public final String principal;
		public final Integer oid;

		private StackableData(String principal, Integer oid) {
			this.principal = principal;
			this.oid = oid;
		}
	}

	private static final ThreadLocal<Deque<StackableData>> STACKS = ThreadLocal.withInitial(ArrayDeque::new);

	private static Deque<StackableData> stack() {
		return STACKS.get();
	}

	public static void pushPrincipal(String username) {
		stack().push(new StackableData(username, null));
	}

	public static void pushPrincipal(String username, int oid) {
		stack().push(new StackableData(username, oid));
	}

	public static void popPrincipal() {
		stack().pop();
	}

	public static String getCurrentPrincipal() {
		final Deque<StackableData> stack = stack();
		if (stack.isEmpty()) {
			final AbstractAuthenticationToken auth = getAuthentication();
			if (auth == null) {
				throw new IllegalStateException(MISSING_AUTH);
			}
			return new PrincipalSid(auth).getPrincipal();
		}
		else {
			return stack.peek().principal;
		}
	}

	public static Integer getCurrentOID() {
		final Deque<StackableData> stack = stack();
		if (!stack.isEmpty()) {
			for (StackableData data : stack) {
				if (data.oid != null) {
					return data.oid;        // override
				}
			}
		}
		final AbstractAuthenticationToken auth = getAuthentication();
		if (auth == null) {
			if (stack.isEmpty()) {
				throw new IllegalStateException(MISSING_AUTH);
			}
			else {
				return null;
			}
		}
		return getDetails(auth).getIfoSecOID();
	}

	private static UniregSecurityDetails getDetails() {
		AbstractAuthenticationToken auth = getAuthentication();
		if (auth == null) {
			throw new IllegalStateException(MISSING_AUTH);
		}
		return getDetails(auth);
	}

	private static UniregSecurityDetails getDetails(@NotNull AbstractAuthenticationToken auth) {
		UniregSecurityDetails d = (UniregSecurityDetails) auth.getDetails();
		if (d == null) {
			d = new UniregSecurityDetails();
			auth.setDetails(d);
		}
		return d;
	}

	public static void setAuthentication(Authentication auth) {
		stack().clear();
		SecurityContextHolder.getContext().setAuthentication(auth);
	}

	public static void resetAuthentication() {
		stack().clear();
		SecurityContextHolder.getContext().setAuthentication(null);
	}

	public static AbstractAuthenticationToken getAuthentication() {
		return (AbstractAuthenticationToken) SecurityContextHolder.getContext().getAuthentication();
	}

	public static boolean hasCurrentPrincipal() {
		return isAuthenticated() || !stack().isEmpty();
	}

	public static boolean isAuthenticated() {
		return getAuthentication() != null;
	}

	public static String getFirstName() {
		final UniregSecurityDetails details = getDetails();
		return details.getIamFirstName();
	}

	public static String getLastName() {
		final UniregSecurityDetails details = getDetails();
		return details.getIamLastName();
	}

	public static void setCurrentOID(int i, String sigle) {
		final UniregSecurityDetails details = getDetails();
		details.setIfoSecOID(i);
		details.setIfoSecOIDSigle(sigle);
	}

	public static String getCurrentOIDSigle() {
		final UniregSecurityDetails details = getDetails();
		return details.getIfoSecOIDSigle();
	}
}
