package ch.vd.uniregctb.migration.pm.indexeur;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import ch.vd.registre.simpleindexer.DocGetter;
import ch.vd.registre.simpleindexer.LuceneData;

public interface Index {

	@FunctionalInterface
	interface SearchCallback {
		void handle(TopDocs hits, DocGetter docGetter) throws Exception;
	}

	/**
	 * Nettoyage par le vide
	 */
	void overwriteIndex();

	/**
	 * Ré-indexe l'entité
	 * @param indexable indexable construit à partir de l'entité
	 */
	void removeThenIndexEntity(LuceneData indexable);

	/**
	 * Indexe l'entité (sans se préoccuper de savoir si elle existe déjà ou pas)
	 * @param indexable indexable construit à partir de l'entité
	 */
	void indexEntity(LuceneData indexable);

	void search(Query query, int maxHits, SearchCallback callback);
}
