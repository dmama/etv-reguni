package ch.vd.uniregctb.indexer.lucene;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.document.Document;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import ch.vd.uniregctb.indexer.DocGetter;
import ch.vd.uniregctb.indexer.IndexerException;

public interface Searcher {

	Document doc(int i) throws IOException;

	void searchAll(Query query, Collector collector) throws IndexerException;

	TopDocs search(Query query, int maxHits) throws IndexerException;

	TopDocs search(String queryString, int maxHits);

	TopDocs search(Long id, String type, int maxHits) throws IndexerException;

	TopDocs search(String typeName, List<String> fieldNames, List<String> fieldValues, int maxHits) throws IndexerException;

	int numDocs();

	DocGetter getDocGetter();
}
