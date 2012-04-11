package ch.vd.uniregctb.indexer.lucene;

import java.io.File;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.indexer.IndexerException;

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
		return new LuceneIndex(new File(indexPath));
	}

	@Override
	public String getIndexPath() throws IndexerException {
		return indexPath;
	}
}
