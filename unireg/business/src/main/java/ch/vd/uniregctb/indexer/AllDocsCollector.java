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
	private int docBase;

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
			// [UNIREG-2597] l'index du document est relatif à l'index reader : il faut ajouter docBase pour avoir l'index de document global
			callback.handle(docBase + doc, docGetter);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void setNextReader(IndexReader reader, int docBase) throws IOException {
		this.docBase = docBase;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}
}
