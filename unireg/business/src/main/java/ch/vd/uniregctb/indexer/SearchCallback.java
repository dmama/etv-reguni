package ch.vd.uniregctb.indexer;

import org.apache.lucene.search.TopDocs;

/**
 * Méthode de callback appelée par les méthodes 'search' du global index.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface SearchCallback {
	void handle(TopDocs hits, DocGetter docGetter) throws Exception;
}
