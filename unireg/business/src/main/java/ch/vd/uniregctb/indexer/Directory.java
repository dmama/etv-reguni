package ch.vd.uniregctb.indexer;

import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.index.IndexWriter;

/**
 * Classe d'interfaçage sur le directory lucene qui contient la logique de locking propre aux accès simultané en lecture et exclusif en
 * écriture (pattern lecteurs/rédacteur).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Directory {

	/** directory lucene */
	public final org.apache.lucene.store.Directory directory;

	/** Lock qui protège l'accès au directory */
	public final ReentrantReadWriteLock lock;

	private LuceneSearcher searcher = null;
	private LuceneWriter writer = null;

	static {
		TokenStream.setOnlyUseNewAPI(true);
	}

	public Directory(org.apache.lucene.store.Directory directory) {
		this.directory = directory;
		// [UNIREG-2287] On utilise le mode 'fair' parce qu'autrement les writers peuvent se trouver en situation de famine
		// prolongée si des readers font des requêtes continuelles à l'indexeur. A noter que l'implémentation 'fair' de ce lock
		// est buggée en java 1.5 (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6315709).
		// Elle part en dead-lock si :
		//  - un thread A obtient le lock en read
		//  - un thread B fait une demande du lock en write
		//  - le thread A fait une demande rééentrante du lock en read
		// Ce bug a été corrigé en java 1.6. Pour l'instant on s'assure parce inspection visuelle du code qu'aucun reader ne fait
		// d'appel réentrant.
		this.lock = new ReentrantReadWriteLock(true);
	}

	public void close() throws IOException {

		if (searcher != null) {
			searcher.close();
			searcher = null;
		}

		if (writer != null) {
			writer.close();
			writer = null;
		}

		if (directory != null) {
			directory.close();
		}
	}

	public void clearLock() throws IOException {
		directory.clearLock(IndexWriter.WRITE_LOCK_NAME);
	}

	/**
	 * Callback utilisé par la méthode {@link Directory#read(ReadOnlyCallback)}.
	 */
	public interface ReadOnlyCallback {
		/**
		 * Cette méthode permet d'effectuer des opérations en read-only sur l'index lucene.
		 * <p>
		 * <b>Note:</b> le searcher spécifié en paramètre ne doit pas être utilisé en dehors de la méthode pour des raisons de
		 * synchronisation d'accès à l'index.
		 *
		 * @param searcher
		 *            le searcher pour effectuer des opérations sur l'index.
		 */
		Object doInReadOnly(LuceneSearcher searcher);
	}

	/**
	 * Callback utilisé par la méthode {@link Directory#write(WriteCallback)}.
	 */
	public interface WriteCallback {
		/**
		 * Cette méthode permet d'effectuer des opérations en read-write sur l'index lucene.
		 * <p>
		 * <b>Note:</b> le writer spécifié en paramètre ne doit pas être utilisé en dehors de la méthode pour des raisons de synchronisation
		 * d'accès à l'index.
		 *
		 * @param writer
		 *            le writer pour effectuer des opérations sur l'index.
		 */
		Object doInWrite(LuceneWriter writer);
	}

	/**
	 * Effectue une recherche en read-only sur l'index lucene.
	 *
	 * @param callback
	 *            la méthode de callback appelée qui doit effectuer les opérations sur l'index lucene.
	 * @return la valeur retournée par l'appel de la méthode {@link ReadOnlyCallback#doInReadOnly(LuceneSearcher)}.
	 */
	public Object read(ReadOnlyCallback callback) {
		acquireReadLock();
		try {
			return callback.doInReadOnly(searcher);
		}
		finally {
			releaseReadLock();
		}
	}

	/**
	 * Effectue une recherche en read-write sur l'index lucene.
	 *
	 * @param callback
	 *            la méthode de callback appelée qui doit effectuer les opérations sur l'index lucene.
	 * @return la valeur retournée par l'appel de la méthode {@link WriteCallback#doInWrite(LuceneWriter)}.
	 */
	public Object write(WriteCallback callback) {
		acquireWriteLock();
		try {
			return callback.doInWrite(writer);
		}
		finally {
			releaseWriteLock();
		}
	}

	/**
	 * Lock l'index lucene en lecture et retourne un searcher.
	 * <p>
	 * Cet appel bloque si l'index est déjà ouvert en écriture. Cet appel ne bloque pas si l'index est déjà ouvert en lecture par d'autre
	 * threads.
	 *
	 * @return un searcher
	 */
	private LuceneSearcher acquireReadLock() {
		lock.readLock().lock();
		try {
			// plusieurs threads peuvent obtenir le read-lock en même temps : on doit donc synchroniser nos données
			synchronized (this) {
				if (writer != null) {
					writer.commit();
				}
				if (searcher == null) {
					searcher = new LuceneSearcher(directory);
				}
			}
		}
		catch (RuntimeException e) {
			lock.readLock().unlock();
			throw e;
		}
		return searcher;
	}

	private void releaseReadLock() {
		lock.readLock().unlock();
	}

	/**
	 * Lock l'index lucene en écriture et retourne un writer.
	 * <p>
	 * Cet appel bloque si l'index est déjà ouvert en lecture ou en écriture par d'autre threads.
	 *
	 * @return un writer
	 */
	private LuceneWriter acquireWriteLock() {
		lock.writeLock().lock();

		try {
			// pas de synchronisation nécessaire : uniquement un thread en écriture par définition
			if (searcher != null) {
				searcher.close();
				searcher = null;
			}
			if (writer == null) {
				writer = new LuceneWriter(directory, false);
			}
		}
		catch (RuntimeException e) {
			lock.writeLock().unlock();
			throw e;
		}
		return writer;
	}

	private void releaseWriteLock() {
		lock.writeLock().unlock();
	}
}
