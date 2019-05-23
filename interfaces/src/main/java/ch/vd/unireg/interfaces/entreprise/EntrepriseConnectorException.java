package ch.vd.unireg.interfaces.entreprise;

/**
 * Exception renvoyée par le connecteur des entreprises lors d'une erreur réseau ou d'un problème de droit d'accès.
 */
public class EntrepriseConnectorException extends RuntimeException {

	private static final long serialVersionUID = -7781497961960662163L;

	public EntrepriseConnectorException(String message) {
		super(message);
	}

	public EntrepriseConnectorException(Throwable e) {
		super(e);
	}

	public EntrepriseConnectorException(String string, Throwable e) {
		super(string, e);
	}
}

