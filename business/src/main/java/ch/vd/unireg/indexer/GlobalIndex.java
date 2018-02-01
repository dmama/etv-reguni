package ch.vd.uniregctb.indexer;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.TopDocs;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.simpleindexer.LuceneException;
import ch.vd.registre.simpleindexer.LuceneIndex;
import ch.vd.registre.simpleindexer.ReadOnlyCallback;
import ch.vd.registre.simpleindexer.Searcher;
import ch.vd.registre.simpleindexer.WriteCallback;
import ch.vd.registre.simpleindexer.Writer;
import ch.vd.uniregctb.indexer.lucene.IndexProvider;

/**
 * Cette classe est le point d'entrée unique vers l'indexeur Lucene. Il gère notamment l'initialisation des ressources, l'accès concurrent
 * aux données en lecture et en écriture.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class GlobalIndex implements InitializingBean, DisposableBean, GlobalIndexInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(GlobalIndex.class);

	public static final String BAD_CONTEXT_MESSAGE =
			"Il n'y a aucun index à écraser: soit l'index n'a pas été initialisé par afterPropertiesSet(), soit le context a été conservé malgré l'échec de cette méthode (possible en test avec Spring).";

	private final IndexProvider provider;
	protected LuceneIndex index = null;

	public GlobalIndex(IndexProvider provider) throws IndexerException {
		this.provider = provider;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		createIndex();
	}

	@Override
	public void destroy() throws Exception {

		if (index != null) {
			closeIndex();
			index = null;
		}
	}

	private void createIndex() {

		// récupération du répertoire sur le disque
		Assert.isNull(index);
		try {
			index = provider.getNewIndex();
			// optimize(); // Prends bcp de temps, pas possible lors de l'open
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw new IndexerException(e);
		}
	}

	private void closeIndex() {

		Assert.notNull(index, BAD_CONTEXT_MESSAGE);
		try {
			index.close();
		}
		catch (Exception e) {
			throw new IndexerException(e);
		}
		finally {
			index = null;
		}
	}

	@Override
	public void overwriteIndex() {

		Assert.notNull(index, BAD_CONTEXT_MESSAGE);
		try {
			index.overwrite();
		}
		catch (Exception e) {
			throw new IndexerException(e);
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

		final Integer count = (Integer) index.read(Searcher::numDocs);

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void optimize() throws IndexerException {
		LOGGER.trace("Optimizing indexer...");

		try {
			index.write(new WriteCallback() {
				@Override
				public Object doInWrite(Writer writer) {
					writer.optimize();
					return null;
				}
			});
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}

		LOGGER.trace("Indexer optimized");
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void flush() throws IndexerException {
		LOGGER.trace("Flushing indexer...");

		try {
			index.write(new WriteCallback() {
				@Override
				public Object doInWrite(Writer writer) {
					writer.commit();
					return null;
				}
			});
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}

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

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.write(new WriteCallback() {
				@Override
				public Object doInWrite(Writer writer) {
					writer.remove(data.id);
					writer.index(data);
					return null;
				}
			});
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}

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

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.write(new WriteCallback() {
				@Override
				public Object doInWrite(Writer writer) {
					for (IndexableData d : data) {
						writer.remove(d.id);
						writer.index(d);
					}
					return null;
				}
			});
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}

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

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.write(new WriteCallback() {
				@Override
				public Object doInWrite(Writer writer) {
					writer.index(data);
					return null;
				}
			});
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}

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

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.write(new WriteCallback() {
				@Override
				public Object doInWrite(Writer writer) {
					for (IndexableData d : data) {
						writer.index(d);
					}
					return null;
				}
			});
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Indexing entities done: ids = " + Arrays.toString(data.toArray()));
		}
	}

	@Override
	public int deleteDuplicate() {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Deleting duplicated entities...");
		}

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return -1;
		}

		final int count;
		try {
			count = index.deleteDuplicate();
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Deleting duplicated entities done.");
		}

		return count;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void removeEntity(final Long id) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Removing entity...");
		}

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.write(new WriteCallback() {
				@Override
				public Object doInWrite(Writer writer) {
					writer.remove(id);
					return null;
				}
			});
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Removing done of " + id);
		}
	}

	@Override
	public void deleteEntitiesMatching(@NotNull Query query) {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Deleting entities matching query " + query+ "...");
		}

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.write(writer -> {
				writer.deleteDocuments(query);
				writer.commit();
				return null;
			});
		}
		catch (LuceneException e) {
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Deleting entities matching query done.");
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

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.read(new ReadOnlyCallback() {
				@Override
				public Object doInReadOnly(final Searcher searcher) throws Exception {
					final TopDocs hits = searcher.search(query, maxHits);
					callback.handle(hits, searcher.getDocGetter());
					return null;
				}
			});
		}
		catch (LuceneException e) {
			// pour ne pas transformer une TooManyException en IndexerException
			if (e.getCause() instanceof IndexerException) {
				throw (IndexerException) e.getCause();
			}
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching done: " + query);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void search(final Query query, final int maxHits, final Sort sort, final SearchCallback callback) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching: " + query);
		}

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.read(new ReadOnlyCallback() {
				@Override
				public Object doInReadOnly(final Searcher searcher) throws Exception {
					final TopDocs hits = searcher.search(query, null, maxHits, sort);
					callback.handle(hits, searcher.getDocGetter());
					return null;
				}
			});
		}
		catch (LuceneException e) {
			// pour ne pas transformer une TooManyException en IndexerException
			if (e.getCause() instanceof IndexerException) {
				throw (IndexerException) e.getCause();
			}
			throw new IndexerException(e);
		}

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

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.read(new ReadOnlyCallback() {
				@Override
				public Object doInReadOnly(Searcher searcher) throws Exception {
					final TopDocs hits = searcher.search(query, maxHits);
					callback.handle(hits, searcher.getDocGetter());
					return null;
				}
			});
		}
		catch (LuceneException e) {
			// pour ne pas transformer une TooManyException en IndexerException
			if (e.getCause() instanceof IndexerException) {
				throw (IndexerException) e.getCause();
			}
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching done: " + query);
		}
	}

	@Override
	public void searchAll(final Query query, final SearchAllCallback callback) throws IndexerException {

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching: " + query);
		}

		if (index == null) {
			LOGGER.warn("L'indexeur n'est pas initialisé" + hashCode());
			return;
		}

		try {
			index.read(new ReadOnlyCallback() {
				@Override
				public Object doInReadOnly(final Searcher searcher) throws Exception {
					final Collector collector = new AllDocsCollector(callback, searcher.getDocGetter());
					searcher.searchAll(query, collector);
					return null;
				}
			});
		}
		catch (LuceneException e) {
			// pour ne pas transformer une TooManyException en IndexerException
			if (e.getCause() instanceof IndexerException) {
				throw (IndexerException) e.getCause();
			}
			throw new IndexerException(e);
		}

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("Searching done: " + query);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getIndexPath() {
		return provider.getIndexPath();
	}
}
