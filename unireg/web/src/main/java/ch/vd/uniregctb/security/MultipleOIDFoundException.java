package ch.vd.uniregctb.security;

import org.acegisecurity.AuthenticationException;

public class MultipleOIDFoundException extends AuthenticationException {

	private static final long serialVersionUID = 7716081770572018516L;

	public MultipleOIDFoundException(String msg, Throwable t) {
		super(msg, t);
	}

	public MultipleOIDFoundException(String msg) {
		super(msg);
	}

}
