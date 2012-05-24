package ch.vd.uniregctb.indexer.lucene;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.FSDirectory;
import org.springframework.util.FileSystemUtils;

import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.OurOwnFrenchAnalyzer;

/**
 * Classe d'interfaçage sur l'index lucene qui contient la logique de locking propre aux accès simultané en lecture et exclusif en
 * écriture (pattern lecteurs/rédacteur).
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class LuceneIndex {

	private static final Logger LOGGER = Logger.getLogger(LuceneIndex.class);

	private static final long WRITE_LOCK_TIMEOUT = 60000; // 60 secondes

	/** directory lucene */
	private org.apache.lucene.store.Directory directory;
	private File directoryPath;

	/** Lock qui protège l'accès au directory */
	private final ReentrantReadWriteLock lock;
	private LuceneSearcher searcher = null;
	private LuceneWriter writer = null;

	static  {
		/**
		 * Par défaut, le timeout est de 1 seconde. Dans ces conditions, la probabilité de timeout lorsque plusieurs threads essaient
		 * d'écrire simultanément dans l'index est assez élevée. En portant ce timeout à 60 secondes, on est pratiquement sûre que s'il y a
		 * un timeout, c'est parce qu'il y a eu un crash d'une application qui a laissé l'index locké.
		 */
		IndexWriter.setDefaultWriteLockTimeout(WRITE_LOCK_TIMEOUT);
	}

	public LuceneIndex(File directoryPath) throws IOException {
		this.directory = FSDirectory.open(directoryPath);
		this.directoryPath = directoryPath;
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

		createDiskStoreIfNeeded();
		clearDiskStoreLock();
	}

	private void createDiskStoreIfNeeded() {

		boolean createIndex = false;

		// vérification que l'index existe sur le disque
		LuceneSearcher ls = null;
		try {
			ls = new LuceneSearcher(directory);
		}
		catch (Exception e) {
			// l'index n'existe pas -> on le crée
			createIndex = true;
		}
		finally {
			if (ls != null) {
				ls.close();
			}
		}

		// création de l'index si nécessaire
		if (createIndex) {
			LOGGER.info("Création d'un nouvel index LUCENE vide");
			LuceneWriter lw = null;
			try {
				lw = new LuceneWriter(directory, true /* create */);
			}
			finally {
				if (lw != null) {
					lw.close();
				}
			}
		}
		else {
			LOGGER.info("Ouverture de l'index LUCENE existant");
		}
	}

	private void clearDiskStoreLock() throws IOException {
		directory.clearLock(IndexWriter.WRITE_LOCK_NAME);
	}

	/**
	 * Ferme l'index courant, supprime toutes les données indexées et réouvre l'index.
	 *
	 * @throws Exception en cas d'erreur d'accès au disque
	 */
	public void overwrite() throws Exception {

		acquireWriteLock();
		try {
			writer.close();
			writer = null;

			// Efface le répertoire
			LOGGER.info("Effacement du répertoire d'indexation" + directoryPath);

			this.directory.close();

			try {
				// en fait, il ne faut effacer que le contenu du répertoire, pas le répertoire lui-même
				// (au cas où celui-ci serait un lien vers un autre endroit du filesystem...)
				deleteDirectoryContent(directoryPath);
			}
			catch (Exception e) {
				LOGGER.error("Exception lors de l'effacement du repertoire: " + e);
				throw e;
			}

			this.directory = FSDirectory.open(directoryPath);

			// on effectue une opération en write pour initialiser l'index
			LuceneWriter w = null;
			try {
				w = new LuceneWriter(directory, false);
				w.deleteDocuments(new Term("", ""));
			}
			catch (IOException ex) {
				throw new IndexerException("Error during deleting a document.", ex);
			}
			finally {
				if (w != null) {
					w.close();
				}
			}
		}
		finally {
			releaseWriteLock();
		}
	}

	private static void deleteDirectoryContent(File dir) {
		final File[] files = dir.listFiles();
		if (files != null && files.length > 0) {
			for (File file : files) {
				FileSystemUtils.deleteRecursively(file);
			}
		}
	}
	
	public void close() throws IOException {

		LOGGER.info("Fermeture de l'index LUCENE.");

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

	/**
	 * Crée et retourne un index writer basé sur l'index lucène courant. L'appelant est responsable de fermer le writer après son utilisation.
	 * <p/>
	 * <b>A n'utiliser que pour le testing !!!</b>
	 *
	 * @return un index writer basé sur l'index lucène courant.
	 * @throws IOException en cas d'erreur d'accès à l'index sur le disque.
	 */
	public IndexWriter createDetachedWriterForTestingOnly() throws IOException {
		return new IndexWriter(directory, new OurOwnFrenchAnalyzer(), false, IndexWriter.MaxFieldLength.LIMITED);
	}

	/**
	 * Effectue une recherche en read-only sur l'index lucene.
	 *
	 * @param callback
	 *            la méthode de callback appelée qui doit effectuer les opérations sur l'index lucene.
	 * @return la valeur retournée par l'appel de la méthode {@link ReadOnlyCallback#doInReadOnly(Searcher)}.
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
	 * @return la valeur retournée par l'appel de la méthode {@link WriteCallback#doInWrite(Writer)}.
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

	public int deleteDuplicate() throws IndexerException {

		acquireWriteLock();
		try {
			LOGGER.info("Recherche et suppression des doublons dans l'index LUCENE.");

			// on ferme temporairement le writer car il prend un lock exclusif sur l'index et pour rechercher/supprimer les doublons ont doit passer par un reader
			writer.close();
			writer = null;

			final LuceneDeduplicator deduplicator = new LuceneDeduplicator(directory);
			return deduplicator.clean();
		}
		catch (IOException e) {
			throw new IndexerException(e);
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
	private Searcher acquireReadLock() {
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
	private Writer acquireWriteLock() {
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
