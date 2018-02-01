package ch.vd.unireg.security;

/**
 * Exception spécifique aux droits d'accès.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DroitAccesException extends Exception {

	private static final long serialVersionUID = 8122032497045553771L;

	public DroitAccesException(String msg, Throwable t) {
		super(msg, t);
	}

	public DroitAccesException(String msg) {
		super(msg);
	}
}
