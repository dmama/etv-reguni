package ch.vd.uniregctb.indexer;

import ch.vd.uniregctb.tiers.Tiers;

/**
 * Exception renvoyée lors d'une erreur dans le moteur d'indexation et de
 * recherche.
 */
public class IndexerException extends RuntimeException {

	private static final long serialVersionUID = 9186744290875325753L;

	private final Tiers tiers;

	/**
	 * @param e l'exception a propager
	 */
	public IndexerException(Exception e) {
		super(e.toString(), e);
		this.tiers = null;
	}

	/**
	 * @param e l'exception a propager
	 */
	public IndexerException(Tiers tiers, Exception e) {
		super(e.toString(), e);
		this.tiers = tiers;
	}

	/**
	 * @param message description de l'exception
	 */
	public IndexerException(String message) {
		super(message);
		this.tiers = null;
	}

	/**
	 * @param message description de l'exception
	 */
	public IndexerException(Tiers tiers, String message) {
		super(message);
		this.tiers = tiers;
	}

	/**
	 * @param message description de l'exception
	 * @param  e l'exception a propager
	 */
	public IndexerException(String message, Exception e) {
		super(message, e);
		this.tiers = null;
	}

	/**
	 * @param message description de l'exception
	 * @param e l'exception a propager
	 */
	public IndexerException(Tiers tiers, String message, Exception e) {
		super(message, e);
		this.tiers = tiers;
	}

	/**
	 * @return le numéro de tiers associé à l'exception; ou <b>null</b> si cette information n'est pas disponible.
	 */
	public Tiers getTiers() {
		return tiers;
	}
}
