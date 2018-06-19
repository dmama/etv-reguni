package ch.vd.unireg.interfaces.entreprise.rcent.adapter.service;

/**
 * @author Raphaël Marmier, 2016-04-14, <raphael.marmier@vd.ch>
 */
public class RCEntAdapterException extends RuntimeException {
	public RCEntAdapterException(String message) {
		super(message);
	}

	public RCEntAdapterException(String message, Throwable cause) {
		super(message, cause);
	}
}
