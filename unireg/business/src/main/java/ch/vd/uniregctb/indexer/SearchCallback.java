package ch.vd.uniregctb.indexer;

import ch.vd.uniregctb.indexer.DocGetter;
import ch.vd.uniregctb.indexer.DocHit;

import java.util.List;

/**
 * Méthode de callback appelée par les méthodes 'search' du global index.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface SearchCallback {
	void handle(List<DocHit> hits, DocGetter docGetter) throws Exception;
}
