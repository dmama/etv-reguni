package ch.vd.uniregctb.role;

/**
 * Exception générique levé pour un service de la couche métier.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ServiceException extends Exception {

	private static final long serialVersionUID = -5014382824794039203L;

	public ServiceException(Throwable e) {
		super(e);
	}

	public ServiceException(String string) {
		super(string);
	}

	public ServiceException(String string, Throwable e) {
		super(string, e);
	}
}
