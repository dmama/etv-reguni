package ch.vd.uniregctb.indexer.lucene;

import java.io.File;

import org.apache.log4j.Logger;

import ch.vd.registre.simpleindexer.LuceneIndex;
import ch.vd.registre.simpleindexer.LuceneIndexImpl;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.indexer.OurOwnFrenchAnalyzer;

public class FSIndexProvider implements IndexProvider {

	private static final Logger LOGGER = Logger.getLogger(FSIndexProvider.class);

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
	public String getIndexPath() throws IndexerException {
		return indexPath;
	}
}
