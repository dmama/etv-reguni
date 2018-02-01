package ch.vd.unireg.indexer.lucene;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.simpleindexer.LuceneIndex;
import ch.vd.registre.simpleindexer.LuceneIndexImpl;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.indexer.OurOwnFrenchAnalyzer;

public class FSIndexProvider implements IndexProvider {

	private static final Logger LOGGER = LoggerFactory.getLogger(FSIndexProvider.class);

	private final String indexPath;

	public FSIndexProvider(String path) throws IndexerException {
		indexPath = path;

		LOGGER.info("L'index Lucene est sauvé dans le système de fichiers: " + indexPath);

		if (this.indexPath.startsWith("${")) {
			// The Path wasn't filtered by maven from ${...} to a real path!
			throw new IndexerException("Path '" + this.indexPath + "' invalid");
		}
	}

	@Override
	public LuceneIndex getNewIndex() throws Exception {
		final LuceneIndexImpl index = new LuceneIndexImpl();
		index.setDirectoryPath(new File(indexPath));
		index.setAnalyzer(new OurOwnFrenchAnalyzer());
		index.open();
		return index;
	}

	@Override
	public String getIndexPath() {
		return indexPath;
	}
}
