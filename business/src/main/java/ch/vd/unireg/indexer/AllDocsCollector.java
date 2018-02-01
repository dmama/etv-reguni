package ch.vd.unireg.indexer;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;

import ch.vd.registre.simpleindexer.DocGetter;

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
	public void setNextReader(AtomicReaderContext context) throws IOException {
		this.docBase = context.docBase;
	}

	@Override
	public boolean acceptsDocsOutOfOrder() {
		return true;
	}
}
