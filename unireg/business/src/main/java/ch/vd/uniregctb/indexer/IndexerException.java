package ch.vd.uniregctb.indexer;

import ch.vd.uniregctb.tiers.Tiers;

/**
 * Exception renvoyée lors d'une erreur dans le moteur d'indexation et de
 * recherche.
 */
public class IndexerException extends RuntimeException {

	/**
	 * Bon bah ouais...
	 */
	private static final long serialVersionUID = 9186744290875325753L;

	private final Tiers tiers;

	/**
	 * @param l'exception a propager
	 */
	public IndexerException(Exception e) {
		super(e.toString(), e);
		this.tiers = null;
	}

	/**
	 * @param l'exception a propager
	 */
	public IndexerException(Tiers tiers, Exception e) {
		super(e.toString(), e);
		this.tiers = tiers;
	}

	/**
	 * @param string
	 */
	public IndexerException(String string) {
		super(string);
		this.tiers = null;
	}

	/**
	 * @param string
	 */
	public IndexerException(Tiers tiers, String string) {
		super(string);
		this.tiers = tiers;
	}

	/**
	 * @param string
	 * @param l'exception a propager
	 */
	public IndexerException(String string, Exception e) {
		super(string, e);
		this.tiers = null;
	}

	/**
	 * @param string
	 * @param l'exception a propager
	 */
	public IndexerException(Tiers tiers, String string, Exception e) {
		super(string, e);
		this.tiers = tiers;
	}

	/**
	 * @return le numéro de tiers associé à l'exception; ou <b>null</b> si cette information n'est pas disponible.
	 */
	public Tiers getTiers() {
		return tiers;
	}
}
