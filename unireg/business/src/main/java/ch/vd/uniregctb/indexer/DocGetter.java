package ch.vd.uniregctb.indexer;

import java.io.IOException;

import org.apache.lucene.document.Document;

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
	 * @throws java.io.IOException en cas d'erreur IO sur l'index
	 */
	Document get(int i) throws IOException;
}
