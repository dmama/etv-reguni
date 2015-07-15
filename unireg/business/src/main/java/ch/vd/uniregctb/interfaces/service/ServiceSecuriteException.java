package ch.vd.uniregctb.interfaces.service;

public class ServiceSecuriteException extends RuntimeException {

	private static final long serialVersionUID = -3173450466162893956L;

	public ServiceSecuriteException(Throwable e) {
		super(e);
	}

	public ServiceSecuriteException(String string, Throwable e) {
		super(string, e);
	}
}
