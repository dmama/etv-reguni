package ch.vd.uniregctb.indexer;

public class TooManyResultsIndexerException extends IndexerException {

	/**
	 *
	 */
	private static final long serialVersionUID = 0L;
	private final int nbResults;

	public TooManyResultsIndexerException(String msg, int nb) {
		super(msg);

		nbResults = nb;
	}

	public int getNbResults() {
		return nbResults;
	}

}
