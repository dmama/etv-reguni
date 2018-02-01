package ch.vd.uniregctb.inbox;

/**
 * Interface implémentée par les éléments expirables
 */
public interface Expirable {

	/**
	 * @return <code>true</code> si l'élément a expiré, <code>false</code> s'il est encore valide
	 */
	boolean isExpired();
}
