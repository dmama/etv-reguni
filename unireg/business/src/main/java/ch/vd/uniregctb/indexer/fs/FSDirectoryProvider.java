package ch.vd.uniregctb.indexer.fs;

import java.io.File;

import org.apache.log4j.Logger;
import org.apache.lucene.store.FSDirectory;

import ch.vd.uniregctb.indexer.Directory;
import ch.vd.uniregctb.indexer.DirectoryProvider;
import ch.vd.uniregctb.indexer.IndexerException;

public class FSDirectoryProvider extends DirectoryProvider {

	private static final Logger LOGGER = Logger.getLogger(FSDirectoryProvider.class);

	private final String indexPath;

	public FSDirectoryProvider(String path) throws IndexerException {
		indexPath = path;

		LOGGER.info("L'index Lucene est sauvé dans le système de fichiers: " + indexPath);

		if (this.indexPath.startsWith("${")) {
			// The Path wasn't filtered by maven from ${...} to a real path!
			throw new IndexerException("Path '" + this.indexPath + "' invalid");
		}
	}

	@Override
	public Directory getNewDirectory() throws Exception {
		final FSDirectory dir = FSDirectory.open(new File(indexPath));
		return new Directory(dir);
	}

	@Override
	public String getIndexPath() throws IndexerException {
		return indexPath;
	}
}
