package ch.vd.uniregctb.indexer.lucene;

/**
 * Callback utilisé par la méthode {@link ch.vd.uniregctb.indexer.lucene.LuceneIndex#write(WriteCallback)}.
 */
public interface WriteCallback {
	/**
	 * Cette méthode permet d'effectuer des opérations en read-write sur l'index lucene.
	 * <p/>
	 * <b>Note:</b> le writer spécifié en paramètre ne doit pas être utilisé en dehors de la méthode pour des raisons de synchronisation d'accès à l'index.
	 *
	 * @param writer le writer pour effectuer des opérations sur l'index.
	 * @return un objet qui sera retourné par la méthode {@link ch.vd.uniregctb.indexer.lucene.LuceneIndex#write(WriteCallback)}.
	 */
	Object doInWrite(Writer writer);
}
