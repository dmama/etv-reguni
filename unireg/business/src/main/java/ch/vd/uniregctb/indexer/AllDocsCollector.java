package ch.vd.uniregctb.indexer;

import java.io.IOException;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

/**
 * Collector lucene qui accepte tous les documents, dans n'importe quel ordre.
 */
public class AllDocsCollector extends Collector {

	private final SearchAllCallback callback;
	private final DocGetter docGetter;

	public AllDocsCollector(SearchAllCallback callback, DocGetter docGetter) {
		this.callback = callback;
		this.docGetter = docGetter;
	}

	@Override
	public void setScorer(Scorer scorer) throws IOException {
	}

	@Override
	public void collect(int doc) throws IOException {
		try {
			callback.handle(doc, docGetter);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}
}
