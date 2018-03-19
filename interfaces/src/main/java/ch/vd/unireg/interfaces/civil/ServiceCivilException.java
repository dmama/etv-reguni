package ch.vd.unireg.interfaces.civil;

/**
 * Exception renvoyée par le service civil dans le d'une erreur de réseau ou d'un problème de droit d'accès.
 */
public class ServiceCivilException extends RuntimeException {

	private static final long serialVersionUID = -5805216933957084601L;

	public ServiceCivilException(String message) {
		super(message);
	}

	public ServiceCivilException(Throwable e) {
		super(e);
	}

	public ServiceCivilException(String string, Throwable e) {
		super(string, e);
	}
}
