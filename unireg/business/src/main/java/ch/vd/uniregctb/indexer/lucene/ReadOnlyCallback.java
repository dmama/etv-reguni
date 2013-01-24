package ch.vd.uniregctb.indexer.lucene;

/**
 * Callback utilisé par la méthode {@link ch.vd.uniregctb.indexer.lucene.LuceneIndex#read(ReadOnlyCallback)}.
 */
public interface ReadOnlyCallback {
	/**
	 * Cette méthode permet d'effectuer des opérations en read-only sur l'index lucene.
	 * <p/>
	 * <b>Note:</b> le searcher spécifié en paramètre ne doit pas être utilisé en dehors de la méthode pour des raisons de synchronisation d'accès à l'index.
	 *
	 * @param searcher le searcher pour effectuer des opérations sur l'index.
	 * @return un objet qui sera retourné par la méthode {@link ch.vd.uniregctb.indexer.lucene.LuceneIndex#read(ReadOnlyCallback)}.
	 */
	Object doInReadOnly(Searcher searcher);
}
