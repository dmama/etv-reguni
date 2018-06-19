package ch.vd.unireg.interfaces.entreprise;

/**
 * Exception renvoyée par le service entreprise lors d'une erreur réseau ou d'un problème de droit d'accès.
 */
public class ServiceEntrepriseException extends RuntimeException {

	private static final long serialVersionUID = -7781497961960662163L;

	public ServiceEntrepriseException(String message) {
		super(message);
	}

	public ServiceEntrepriseException(Throwable e) {
		super(e);
	}

	public ServiceEntrepriseException(String string, Throwable e) {
		super(string, e);
	}
}

