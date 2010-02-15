package ch.vd.uniregctb.indexer;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
			directoryReader = IndexReader.open(directory);
			is = new IndexSearcher(directoryReader);
		}
		catch (Exception e) {
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

	public List<DocHit> search(Query query) throws IndexerException {

		if (query == null) {
			throw new IndexerException("Received empty query expression!");
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Lucene query: '" + query.toString() + "'");
		}

		final List<DocHit> hits = new ArrayList<DocHit>();
		try {
			is.search(query, new HitCollector() {
				@Override
				public void collect(int doc, float score) {
					if (score > 0.0f) {
						hits.add(new DocHit(doc, score));
					}
				}
			});
		}
		catch (BooleanQuery.TooManyClauses e) {
			throw new TooManyResultsIndexerException(e.getMessage(), -1);
		}
		catch (IndexerException e) {
			throw e;
		}
		catch (Exception e) {
			LOGGER.info("Error in query: " + query.toString());
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Found " + hits.size() + " documents matching query");
		}
		return hits;
	}

	public List<DocHit> search(String queryString) {

		List<DocHit> hits;
		if (queryString == null || queryString.equals("")) {
			throw new IndexerException("Received empty query expression!");
		}

		try {
			Analyzer an = getFrenchAnalyzer();
			QueryParser queryParser = new QueryParser(null, an);

			Query query = queryParser.parse(queryString);
			hits = search(query);
		}
		catch (IndexerException ie) {
			throw ie;
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
		return hits;
	}

	public List<DocHit> search(Long id, String type) throws IndexerException {
		// String query = "+" + F_ENTITYID+ ":" + id + " +" + F_DOCTYPE + ":" +
		// type;
		BooleanQuery query = new BooleanQuery();
		TermQuery sub = new TermQuery(new Term(F_ENTITYID, id.toString()));
		query.add(sub, BooleanClause.Occur.MUST);
		sub = new TermQuery(new Term(F_DOCTYPE, type));
		query.add(sub, BooleanClause.Occur.MUST);
		return search(query);
	}

	public List<DocHit> search(String typeName, List<String> fieldNames, List<String> fieldValues) throws IndexerException {

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

		return search(queryString);
	}

	public int numDocs() {
		return directoryReader.numDocs();
	}
}
