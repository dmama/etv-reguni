package ch.vd.uniregctb.interfaces.service;


/**
 * Wrapper autour d'une exception venant du host de manière à la rendre 'run-time'.
 */
public class ServiceInfrastructureException extends RuntimeException {

	private static final long serialVersionUID = 4696534872517976997L;

	public ServiceInfrastructureException(Throwable e) {
		super(e);
	}

	public ServiceInfrastructureException(String message) {
		super(message);
	}

	public ServiceInfrastructureException(String string, Throwable e) {
		super(string, e);
	}
}
