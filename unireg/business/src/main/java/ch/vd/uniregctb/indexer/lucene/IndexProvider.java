package ch.vd.uniregctb.indexer.lucene;

public interface IndexProvider {

	LuceneIndex getNewIndex() throws Exception;
	String getIndexPath() throws Exception;
}
