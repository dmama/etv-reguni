package ch.vd.uniregctb.indexer.lucene;

import ch.vd.registre.simpleindexer.LuceneIndex;

public interface IndexProvider {

	LuceneIndex getNewIndex() throws Exception;
	String getIndexPath();
}
