package ch.vd.unireg.jms;

import ch.vd.technical.esb.EsbMessage;
import ch.vd.technical.esb.util.exception.ESBValidationException;
import ch.vd.unireg.stats.ServiceTracing;

/**
 * Implémentation de tracing de la validation des messages ESB sortants
 */
public class EsbMessageValidatorTracing implements EsbMessageValidator {

	private EsbMessageValidator target;
	private ServiceTracing serviceTracing;

	@SuppressWarnings("UnusedDeclaration")
	public void setTarget(EsbMessageValidator target) {
		this.target = target;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setServiceTracing(ServiceTracing serviceTracing) {
		this.serviceTracing = serviceTracing;
	}

	public EsbMessageValidatorTracing() {
	}

	public EsbMessageValidatorTracing(ServiceTracing serviceTracing, EsbMessageValidator target) {
		this.serviceTracing = serviceTracing;
		this.target = target;
	}

	@Override
	public void validate(EsbMessage msg) throws ESBValidationException {
		Throwable t = null;
		final long start = serviceTracing.start();
		try {
			target.validate(msg);
		}
		catch (ESBValidationException | RuntimeException | Error e) {
			t = e;
			throw e;
		}
		finally {
			serviceTracing.end(start, t, "validate", null);
		}
	}
}
