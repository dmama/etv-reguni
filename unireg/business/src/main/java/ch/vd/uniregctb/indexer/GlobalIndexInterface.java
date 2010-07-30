package ch.vd.uniregctb.indexer;


import java.util.List;

import org.apache.lucene.search.Query;


public interface GlobalIndexInterface {

	/**
	 * Efface l'index.
	 */
	void overwriteIndex();

	/**
	 * La méthode docCount renvoie le nombre de documents dans l'index, qui comprend aussi les documents effacés et non-purgés
	 * optimize() purge l'index donc si on fait un optimize() avant un docCount() on a le nombre de documents exact
	 *
	 * @return le nombre de documents dans l'index
	 */
	int getExactDocCount();

	/**
	 * La méthode docCount renvoie le nombre de documents dans l'index, qui comprend aussi les documents effacés et non-purgés
	 *
	 * @return le nombre de documents dans l'index, y compris les effacés
	 */
	int getApproxDocCount();

	void optimize() throws IndexerException;

	void flush() throws IndexerException;

	void removeThenIndexEntity(IndexableData data);

	void removeThenIndexEntities(List<IndexableData> data);

	void indexEntity(IndexableData data);

	void indexEntities(List<IndexableData> data);

	void removeEntity(Long id, String type) throws IndexerException;

	void search(Query query, int maxHits, SearchCallback callback) throws IndexerException;

	void search(String query, int maxHits, SearchCallback callback) throws IndexerException;

	void searchAll(Query query, SearchAllCallback callback) throws IndexerException;

	/**
	 * @return the indexPath
	 */
	String getIndexPath() throws Exception;

}
