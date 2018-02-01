package ch.vd.unireg.indexer;

import org.apache.lucene.search.TopDocs;

import ch.vd.registre.simpleindexer.DocGetter;

/**
 * Méthode de callback appelée par les méthodes 'search' du global index.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface SearchCallback {
	void handle(TopDocs hits, DocGetter docGetter) throws Exception;
}
