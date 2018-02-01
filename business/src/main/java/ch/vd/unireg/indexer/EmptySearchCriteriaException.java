package ch.vd.uniregctb.indexer;

/**
 * Exception lancée lorsque les critères de recherche sont vides
 */
public class EmptySearchCriteriaException extends IndexerException {

	public EmptySearchCriteriaException(String string) {
		super(string);
	}
}
