package ch.vd.uniregctb.indexer;

public class TooManyClausesIndexerException extends IndexerException {

	private static final long serialVersionUID = 5946545262121631470L;

	public TooManyClausesIndexerException(Exception e) {
		super(e);
	}

	public TooManyClausesIndexerException(String msg) {
		super(msg);
	}
}