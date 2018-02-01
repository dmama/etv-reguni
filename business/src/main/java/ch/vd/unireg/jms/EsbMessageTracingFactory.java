package ch.vd.unireg.jms;

import ch.vd.technical.esb.EsbMessage;

public interface EsbMessageTracingFactory {

	/**
	 * Encapsule le message donné en source dans une structure qui trace les appels importants
	 * @param src le message original
	 * @return l'encapsulation
	 * @see ch.vd.unireg.jms.EsbMessageTracingFacade
	 */
	EsbMessage wrap(EsbMessage src);

}
