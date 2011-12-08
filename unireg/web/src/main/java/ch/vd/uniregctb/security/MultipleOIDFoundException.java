package ch.vd.uniregctb.security;

import org.springframework.security.core.AuthenticationException;

public class MultipleOIDFoundException extends AuthenticationException {

	private static final long serialVersionUID = -7099048733355638873L;
	private final String initialUrl;

	public MultipleOIDFoundException(String msg, String initialUrl) {
		super(msg);
		this.initialUrl = initialUrl;
	}

	public String getInitialUrl() {
		return initialUrl;
	}
}
