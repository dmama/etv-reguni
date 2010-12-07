package ch.vd.uniregctb.indexer;

import java.io.IOException;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.util.Version;

/**
 * Wrapper autour d'un index searcher lucene qui permet d'effectuer des recherches selon les besoins d'Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class LuceneSearcher extends LuceneEngine {

	private static final Logger LOGGER = Logger.getLogger(LuceneSearcher.class);

	private IndexReader directoryReader;
	private IndexSearcher is;

	public final DocGetter docGetter = new DocGetter() {
		public Document get(int i) throws IOException {
			return is.doc(i);
		}
	};

	public LuceneSearcher(Directory directory) throws IndexerException {
		try {
			directoryReader = IndexReader.open(directory, true);
			is = new IndexSearcher(directoryReader);
		}
		catch (Exception e) {
			close();
			throw new IndexerException(e);
		}
	}

	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		close();
	}

	@Override
	public void close() throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("CALL close()");
		}

		try {
			if (is != null) {
				is.close();
			}
			if (directoryReader != null) {
				directoryReader.close();
			}
			is = null;
			directoryReader = null;
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("END close()");
		}
	}

	public Document doc(int i) throws IOException {
		return is.doc(i);
	}

	public void searchAll(Query query, Collector collector) throws IndexerException {

		if (query == null) {
			throw new IndexerException("Received empty query expression!");
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Lucene query: '" + query.toString() + "'");
		}

		try {
			is.search(query, collector);
		}
		catch (BooleanQuery.TooManyClauses e) {
			throw new TooManyClausesIndexerException(e.getMessage());
		}
		catch (IndexerException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.info("Error in query: " + query.toString());
			throw new IndexerException(e);
		}
	}

	public TopDocs search(Query query, int maxHits) throws IndexerException {

		if (query == null) {
			throw new IndexerException("Received empty query expression!");
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Lucene query: '" + query.toString() + "'");
		}

		final TopDocs topDocs;
		try {
			topDocs = is.search(query, maxHits);
		}
		catch (BooleanQuery.TooManyClauses e) {
			throw new TooManyClausesIndexerException(e.getMessage());
		}
		catch (IndexerException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.info("Error in query: " + query.toString());
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Found " + topDocs.totalHits + " documents matching query");
		}

		return topDocs;
	}

	public TopDocs search(String queryString, int maxHits) {

		if (queryString == null || queryString.equals("")) {
			throw new IndexerException("Received empty query expression!");
		}

		TopDocs hits;
		try {
			Analyzer an = getFrenchAnalyzer();
			QueryParser queryParser = new QueryParser(Version.LUCENE_29, null, an);

			Query query = queryParser.parse(queryString);
			hits = search(query, maxHits);
		}
		catch (IndexerException ie) {
			throw ie;
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
		return hits;
	}

	public TopDocs search(Long id, String type, int maxHits) throws IndexerException {
		// String query = "+" + F_ENTITYID+ ":" + id + " +" + F_DOCTYPE + ":" +
		// type;
		BooleanQuery query = new BooleanQuery();
		TermQuery sub = new TermQuery(new Term(F_ENTITYID, id.toString()));
		query.add(sub, BooleanClause.Occur.MUST);
		sub = new TermQuery(new Term(F_DOCTYPE, type));
		query.add(sub, BooleanClause.Occur.MUST);
		return search(query, maxHits);
	}

	public TopDocs search(String typeName, List<String> fieldNames, List<String> fieldValues, int maxHits) throws IndexerException {

		String queryString = "";

		if (typeName != null) {
			queryString = F_DOCTYPE + ":" + typeName;
		}
		for (int i = 0; i < fieldNames.size(); i++) {
			if (queryString.length() > 0) {
				queryString += " AND ";
			}
			queryString += fieldNames.get(i).toUpperCase() + ":\"" + fieldValues.get(i) + "\"";
		}

		return search(queryString, maxHits);
	}

	public int numDocs() {
		return directoryReader.numDocs();
	}
}
