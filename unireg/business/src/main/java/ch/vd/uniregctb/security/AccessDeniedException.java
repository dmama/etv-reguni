package ch.vd.uniregctb.security;

public class AccessDeniedException extends RuntimeException {

	private static final long serialVersionUID = -2430759010720236223L;

	public AccessDeniedException(String msg, Throwable t) {
		super(msg, t);
	}
	
	public AccessDeniedException(String msg){
		super(msg);
	}
}
