package ch.vd.uniregctb.indexer;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.indexer.Directory.ReadOnlyCallback;
import ch.vd.uniregctb.indexer.Directory.WriteCallback;
import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Cette classe est le point d'entrée unique vers l'indexeur Lucene. Il gère notamment l'initialisation des ressources, l'accès concurrent
 * aux données en lecture et en écriture.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class GlobalIndex implements InitializingBean, DisposableBean, GlobalIndexInterface {

	private static final Logger LOGGER = Logger.getLogger(GlobalIndex.class);

	private static final long WRITE_LOCK_TIMEOUT = 60000; // 60 secondes

	private final DirectoryProvider provider;
	protected Directory directory = null;

	public GlobalIndex(DirectoryProvider provider) throws IndexerException {
		this.provider = provider;

		/**
		 * Par défaut, le timeout est de 1 seconde. Dans ces conditions, la probabilité de timeout lorsque plusieurs threads essaient
		 * d'écrire simultanément dans l'index est assez élevée. En portant ce timeout à 60 secondes, on est pratiquement sûre que s'il y a
		 * un timeout, c'est parce qu'il y a eu un crash d'une application qui a laissé l'index locké.
		 */
		IndexWriter.setDefaultWriteLockTimeout(WRITE_LOCK_TIMEOUT);
	}

	public void afterPropertiesSet() throws Exception {
		createDirectory();
	}

	public void destroy() throws Exception {

		if (directory != null) {
			closeDirectory();
			directory = null;
		}
	}

	private void createDirectory() {

		// récupération du répertoire sur le disque
		Assert.isNull(directory);
		try {
			directory = provider.getNewDirectory();
			directory.clearLock();
			// optimize(); // Prends bcp de temps, pas possible lors de l'open
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw new IndexerException(e);
		}

		boolean createIndex = false;

		// vérification que l'index existe sur le disque
		LuceneSearcher searcher = null;
		try {
			searcher = new LuceneSearcher(directory.directory);
		}
		catch (Exception e) {
			// l'index n'existe pas -> on le crée
			createIndex = true;
		}
		finally {
			if (searcher != null) {
				searcher.close();
			}
		}

		// création de l'index si nécessaire
		if (createIndex) {
			LOGGER.info("Création d'un nouvel index LUCENE vide");
			LuceneWriter writer = null;
			try {
				writer = new LuceneWriter(directory.directory, true /* create */);
			}
			finally {
				if (writer != null) {
					writer.close();
				}
			}
		}
		else {
			LOGGER.info("Ouverture de l'index LUCENE existant");
		}
	}

	private void closeDirectory() {

		Assert.notNull(directory);
		try {
			directory.clearLock();
			directory.close();
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
		finally {
			directory = null;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void overwriteIndex() {

		directory.write(new WriteCallback() {
			public Object doInWrite(LuceneWriter writer) {
				writer.close();

				closeDirectory();
				try {
					// Efface le repertoire
					LOGGER.info("Effacement du répertoire d'indexation: " + provider.getIndexPath());
					FileSystemUtils.deleteRecursively(new File(provider.getIndexPath()));
				}
				catch (Exception e) {
					LOGGER.error("Exception lors de l'effacement du repertoire: " + e);
				}
				createDirectory();

				LuceneWriter w = null;
				try {
					w = new LuceneWriter(directory.directory, false);
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

				return null;
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public int getExactDocCount() {
		optimize();
		return getApproxDocCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings({"UnnecessaryLocalVariable"})
	public int getApproxDocCount() {

		final Integer count = (Integer) directory.read(new ReadOnlyCallback() {
			public Object doInReadOnly(LuceneSearcher searcher) {
				return searcher.numDocs();
			}
		});

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	public void optimize() throws IndexerException {
		LOGGER.trace("Optimizing indexer...");

		directory.write(new WriteCallback() {
			public Object doInWrite(LuceneWriter writer) {
				writer.optimize();
				return null;
			}
		});

		LOGGER.trace("Indexer optimized");
	}

	/**
	 * {@inheritDoc}
	 */
	public void flush() throws IndexerException {
		LOGGER.trace("Flushing indexer...");

		directory.write(new WriteCallback() {
			public Object doInWrite(LuceneWriter writer) {
				writer.commit();
				return null;
			}
		});

		LOGGER.trace("Indexer flushed");
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeThenIndexEntity(final IndexableData data) {
		Assert.notNull(data);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Removing and indexing entity: id = " + data.getId());
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.write(new WriteCallback() {
			public Object doInWrite(LuceneWriter writer) {
				writer.remove(data);
				writer.index(data);
				return null;
			}
		});

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Removing and indexing done entity: id = " + data.getId());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeThenIndexEntities(final List<IndexableData> data) {
		Assert.notNull(data);

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.write(new WriteCallback() {
			public Object doInWrite(LuceneWriter writer) {
				for (IndexableData d : data) {
					writer.remove(d);
					writer.index(d);
				}
				return null;
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public void indexEntity(final IndexableData data) {
		Assert.notNull(data);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Indexing entity: id = " + data.getId());
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.write(new WriteCallback() {
			public Object doInWrite(LuceneWriter writer) {
				writer.index(data);
				return null;
			}
		});

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Indexing done entity: id = " + data.getId());
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void indexEntities(final List<IndexableData> data) {
		Assert.notNull(data);

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.write(new WriteCallback() {
			public Object doInWrite(LuceneWriter writer) {
				for (IndexableData d : data) {
					writer.index(d);
				}
				return null;
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	public void removeEntity(final Long id, final String type) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Removing entity...");
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.write(new WriteCallback() {
			public Object doInWrite(LuceneWriter writer) {
				writer.remove(id, type);
				return null;
			}
		});

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Removing done of " + type + "-" + id);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void search(final Query query, final SearchCallback callback) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching: " + query);
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.read(new ReadOnlyCallback() {
			public Object doInReadOnly(final LuceneSearcher searcher) {
				try {
					final List<DocHit> hits = searcher.search(query);
					callback.handle(hits, searcher.docGetter);
				}
				catch (IndexerException e) {
					// pour ne pas transformer une TooManyException en IndexerException
					throw e;
				}
				catch (Exception e) {
					throw new IndexerException(e);
				}
				return null;
			}
		});

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching done: " + query);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void search(final String query, final SearchCallback callback) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching: " + query);
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.read(new ReadOnlyCallback(){
			public Object doInReadOnly(LuceneSearcher searcher) {
				try {
					final List<DocHit> hits = searcher.search(query);
					callback.handle(hits, searcher.docGetter);
				}
				catch (IndexerException e) {
					// pour ne pas transformer une TooManyException en IndexerException
					throw e;
				}
				catch (Exception e) {
					throw new IndexerException(e);
				}
				return null;
			}
		});

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching done: " + query);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public String getIndexPath() throws Exception {
		return provider.getIndexPath();
	}
}
