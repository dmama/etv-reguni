package ch.vd.uniregctb.indexer;

import java.util.HashMap;

public interface SubIndexable {
	
	/**
	 * Return the list of values of one or more entity of a document
	 * @return
	 */
	public HashMap<String, String> getKeyValues() throws IndexerException;

}
