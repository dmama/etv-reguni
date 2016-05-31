package ch.vd.uniregctb.migration.pm.indexeur;

import java.io.File;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.simpleindexer.LuceneData;
import ch.vd.registre.simpleindexer.LuceneIndex;
import ch.vd.registre.simpleindexer.LuceneIndexImpl;
import ch.vd.uniregctb.indexer.OurOwnFrenchAnalyzer;

/**
 * Indexeur (interface technique)
 */
public class IndexImpl implements InitializingBean, DisposableBean, Index {

	private static final Logger LOGGER = LoggerFactory.getLogger(IndexImpl.class);

	private final String indexPath;

	private LuceneIndex indexeur = null;

	public IndexImpl(String indexPath) {
		if (indexPath.startsWith("${")) {
			// The Path wasn't filtered by maven from ${...} to a real path!
			throw new IllegalArgumentException("Path '" + indexPath + "' invalid");
		}

		this.indexPath = indexPath;
		LOGGER.info("L'index Lucene est sauvé dans le système de fichiers: " + indexPath);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		indexeur = buildIndex(indexPath);
	}

	@Override
	public void destroy() throws Exception {
		if (indexeur != null) {
			indexeur.close();
			indexeur = null;
		}
	}

	private static LuceneIndex buildIndex(String indexPath) {
		final LuceneIndexImpl index = new LuceneIndexImpl();
		index.setDirectoryPath(new File(indexPath));
		index.setAnalyzer(new OurOwnFrenchAnalyzer());
		index.open();
		return index;
	}

	@Override
	public void overwriteIndex() {
		indexeur.overwrite();
	}

	@Override
	public void removeThenIndexEntity(LuceneData data) {
		indexeur.write(writer -> {
			writer.remove(data.getId());
			writer.index(data);
			return null;
		});
	}

	@Override
	public void indexEntity(LuceneData data) {
		indexeur.write(writer -> {
			writer.index(data);
			return null;
		});
	}

	@Override
	public void search(Query query, int maxHits, SearchCallback callback) {
		indexeur.read(searcher -> {
			final TopDocs hits = searcher.search(query, maxHits);
			callback.handle(hits, searcher.getDocGetter());
			return null;
		});
	}
}
