package ch.vd.unireg.common;

/**
 * Interface de génération de noms pour des threads
 */
public interface ThreadNameGenerator {

	/**
	 * @return Un nouveau nom de thread
	 */
	String getNewThreadName();
}
