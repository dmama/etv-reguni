package ch.vd.unireg.interfaces.civil;

/**
 * Exception renvoyée par le connecteur des individus dans le cas d'une erreur de réseau ou d'un problème de droit d'accès.
 */
public class IndividuConnectorException extends RuntimeException {

	private static final long serialVersionUID = -5805216933957084601L;

	public IndividuConnectorException(String message) {
		super(message);
	}

	public IndividuConnectorException(Throwable e) {
		super(e);
	}

	public IndividuConnectorException(String string, Throwable e) {
		super(string, e);
	}
}

