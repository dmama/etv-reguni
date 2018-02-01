package ch.vd.unireg.indexer.lucene;

import ch.vd.registre.simpleindexer.LuceneIndex;

public interface IndexProvider {

	LuceneIndex getNewIndex() throws Exception;
	String getIndexPath();
}
