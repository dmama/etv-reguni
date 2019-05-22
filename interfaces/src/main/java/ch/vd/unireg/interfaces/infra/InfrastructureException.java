package ch.vd.unireg.interfaces.infra;


/**
 * Wrapper autour d'une exception venant du host de manière à la rendre 'run-time'.
 */
public class InfrastructureException extends RuntimeException {

	private static final long serialVersionUID = 4696534872517976997L;

	public InfrastructureException(Throwable e) {
		super(e);
	}

	public InfrastructureException(String message) {
		super(message);
	}

	public InfrastructureException(String string, Throwable e) {
		super(string, e);
	}
}
