package ch.vd.unireg.jms;

import ch.vd.technical.esb.EsbMessage;

/**
 * Interface implémentée par un EsbMessage qui en encapsule un autre (proxy, cache...)
 */
public interface EsbMessageWrapper {

	/**
	 * @return l'objet immédiatement encapsulé
	 */
	EsbMessage getTarget();

	/**
	 * @return l'objet encapsulé tout en bout de chaîne
	 */
	EsbMessage getUltimateTarget();
}
