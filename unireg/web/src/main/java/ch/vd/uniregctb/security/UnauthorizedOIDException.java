package ch.vd.uniregctb.security;

import org.springframework.security.core.AuthenticationException;

public class UnauthorizedOIDException extends AuthenticationException {

	private static final long serialVersionUID = -6094999420875059197L;
	private final String initialUrl;

	public UnauthorizedOIDException(String msg, String initialUrl) {
		super(msg);
		this.initialUrl = initialUrl;
	}

	public String getInitialUrl() {
		return initialUrl;
	}
}
