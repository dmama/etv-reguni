package ch.vd.uniregctb.common;

/**
 * Interface de génération de noms pour des threads
 */
public interface ThreadNameGenerator {

	/**
	 * @return Un nouveau nom de thread
	 */
	String getNewThreadName();
}
