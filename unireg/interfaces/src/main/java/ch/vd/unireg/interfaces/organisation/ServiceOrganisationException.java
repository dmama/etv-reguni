package ch.vd.unireg.interfaces.organisation;

/**
 * Exception renvoyée par le service organisation lors d'une erreur réseau ou d'un problème de droit d'accès.
 */
public class ServiceOrganisationException extends RuntimeException {

	private static final long serialVersionUID = -7781497961960662163L;

	public ServiceOrganisationException(String message) {
		super(message);
	}

	public ServiceOrganisationException(Throwable e) {
		super(e);
	}

	public ServiceOrganisationException(String string, Throwable e) {
		super(string, e);
	}
}

