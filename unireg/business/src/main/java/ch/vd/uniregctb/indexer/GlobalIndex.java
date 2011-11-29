package ch.vd.uniregctb.indexer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.FileSystemUtils;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.indexer.Directory.ReadOnlyCallback;
import ch.vd.uniregctb.indexer.Directory.WriteCallback;

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

	@Override
	public void afterPropertiesSet() throws Exception {
		createDirectory();
	}

	@Override
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
	@Override
	public void overwriteIndex() {

		directory.write(new WriteCallback() {
			@Override
			public Object doInWrite(LuceneWriter writer) {
				writer.close();

				closeDirectory();
				try {
					// Efface le repertoire
					LOGGER.info("Effacement du répertoire d'indexation: " + provider.getIndexPath());

					// en fait, il ne faut effacer que le contenu du répertoire, pas le répertoire lui-même
					// (au cas où celui-ci serait un lien vers un autre endroit du filesystem...)
					deleteDirectoryContent(new File(provider.getIndexPath()));
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

	private static void deleteDirectoryContent(File dir) {
		final File[] files = dir.listFiles();
		if (files != null && files.length > 0) {
			for (File file : files) {
				FileSystemUtils.deleteRecursively(file);
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int getExactDocCount() {
		optimize();
		return getApproxDocCount();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	@SuppressWarnings({"UnnecessaryLocalVariable"})
	public int getApproxDocCount() {

		final Integer count = (Integer) directory.read(new ReadOnlyCallback() {
			@Override
			public Object doInReadOnly(LuceneSearcher searcher) {
				return searcher.numDocs();
			}
		});

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void optimize() throws IndexerException {
		LOGGER.trace("Optimizing indexer...");

		directory.write(new WriteCallback() {
			@Override
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
	@Override
	public void flush() throws IndexerException {
		LOGGER.trace("Flushing indexer...");

		directory.write(new WriteCallback() {
			@Override
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
	@Override
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
			@Override
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
	@Override
	public void removeThenIndexEntities(final List<IndexableData> data) {
		Assert.notNull(data);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Re-indexing entities: ids = " + Arrays.toString(data.toArray()));
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.write(new WriteCallback() {
			@Override
			public Object doInWrite(LuceneWriter writer) {
				for (IndexableData d : data) {
					writer.remove(d);
					writer.index(d);
				}
				return null;
			}
		});

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Re-indexing entities done: ids = " + Arrays.toString(data.toArray()));
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
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
			@Override
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
	@Override
	public void indexEntities(final List<IndexableData> data) {
		Assert.notNull(data);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Indexing entities: ids = " + Arrays.toString(data.toArray()));
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.write(new WriteCallback() {
			@Override
			public Object doInWrite(LuceneWriter writer) {
				for (IndexableData d : data) {
					writer.index(d);
				}
				return null;
			}
		});

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Indexing entities done: ids = " + Arrays.toString(data.toArray()));
		}
	}

	@Override
	public int deleteDuplicate() {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Deleting duplicated entities...");
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return -1;
		}

		final int count = (Integer) directory.write(new WriteCallback() {
			@Override
			public Object doInWrite(LuceneWriter writer) {
				return writer.deleteDuplicate();
			}
		});

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Deleting duplicated entities done.");
		}

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeEntity(final Long id, final String type) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Removing entity...");
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.write(new WriteCallback() {
			@Override
			public Object doInWrite(LuceneWriter writer) {
				writer.remove(id, type);
				return null;
			}
		});

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Removing done of " + type + '-' + id);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void search(final Query query, final int maxHits, final SearchCallback callback) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching: " + query);
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.read(new ReadOnlyCallback() {
			@Override
			public Object doInReadOnly(final LuceneSearcher searcher) {
				try {
					final TopDocs hits = searcher.search(query, maxHits);
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
	@Override
	public void search(final String query, final int maxHits, final SearchCallback callback) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching: " + query);
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.read(new ReadOnlyCallback(){
			@Override
			public Object doInReadOnly(LuceneSearcher searcher) {
				try {
					final TopDocs hits = searcher.search(query, maxHits);
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

	@Override
	public void searchAll(final Query query, final SearchAllCallback callback) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching: " + query);
		}

		if (directory == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		directory.read(new ReadOnlyCallback() {
			@Override
			public Object doInReadOnly(final LuceneSearcher searcher) {
				try {
					final Collector collector = new AllDocsCollector(callback, searcher.docGetter);
					searcher.searchAll(query, collector);
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
	@Override
	public String getIndexPath() throws Exception {
		return provider.getIndexPath();
	}
}
