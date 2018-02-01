package ch.vd.unireg.indexer;

import ch.vd.registre.simpleindexer.DocGetter;

/**
 * Méthode de callback appelée par les méthodes 'search' du global index.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface SearchAllCallback {
	void handle(int doc, DocGetter docGetter) throws Exception;
}