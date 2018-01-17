package ch.vd.uniregctb.indexer;


import java.util.List;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.jetbrains.annotations.NotNull;


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

	/**
	 * Supprime les éléments dupliqués (doublons).
	 * @return le nombre d'éléments supprimés.
	 */
	int deleteDuplicate();

	void removeEntity(Long id) throws IndexerException;

	/**
	 * Supprime de l'indexe les éléments qui correspondent à la query spécifiée.
	 *
	 * @param query une query
	 */
	void deleteEntitiesMatching(@NotNull Query query);

	/**
	 * Recherche les <i>maxHits</i> meilleures résultats.
	 *
	 * @param query    les critères de recherche
	 * @param maxHits  le nombre maximal de résultats retournés.
	 * @param callback le callback appelé sur chacun des résultats trouvés
	 * @throws IndexerException en cas d'erreur dans l'indexeur
	 */
	void search(Query query, int maxHits, SearchCallback callback) throws IndexerException;

	/**
	 * Recherche les <i>maxHits</i> meilleurs résultats triés par un critère spécifique
	 * @param query les critères de recherche
	 * @param maxHits le nombre maximal de résultats retournés
	 * @param sort le critère de tri
	 * @param callback le callback appelé sur chacun des résultats trouvés
	 * @throws IndexerException en cas d'erreur dans l'indexeur
	 */
	void search(Query query, int maxHits, Sort sort, SearchCallback callback) throws IndexerException;

	void search(String query, int maxHits, SearchCallback callback) throws IndexerException;

	void searchAll(Query query, SearchAllCallback callback) throws IndexerException;

	/**
	 * @return the indexPath
	 */
	String getIndexPath();
}
