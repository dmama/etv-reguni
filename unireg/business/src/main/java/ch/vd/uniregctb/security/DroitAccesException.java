package ch.vd.uniregctb.security;

/**
 * Exception spécifique aux droits d'accès.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class DroitAccesException extends Exception {

	private static final long serialVersionUID = -1758850613746806898L;

	public DroitAccesException(String msg, Throwable t) {
		super(msg, t);
	}

	public DroitAccesException(String msg) {
		super(msg);
	}
}
