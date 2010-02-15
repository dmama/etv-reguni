package ch.vd.uniregctb.indexer;

import org.apache.lucene.document.Document;

import java.io.IOException;

/**
 * Interface permettant de récupérer un document lucene à partir de son id.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface DocGetter {

	/**
	 * Méthode permettant de récupérer un document lucene à partir de son id.
	 *
	 * @param i l'id du document lucene
	 * @return un document
	 */
	Document get(int i) throws IOException;
}
