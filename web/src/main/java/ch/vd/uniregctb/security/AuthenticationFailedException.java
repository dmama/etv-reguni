package ch.vd.uniregctb.security;

import org.springframework.security.core.AuthenticationException;

public class AuthenticationFailedException extends AuthenticationException {

	private static final long serialVersionUID = 2752064921389178440L;

	public AuthenticationFailedException(String msg, Throwable t) {
		super(msg, t);
	}
	
	public AuthenticationFailedException(String msg){
		super(msg);
	}
}
