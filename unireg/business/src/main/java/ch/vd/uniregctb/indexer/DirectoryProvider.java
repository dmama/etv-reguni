package ch.vd.uniregctb.indexer;

public abstract class DirectoryProvider {

	public abstract Directory getNewDirectory() throws Exception;
	public abstract String getIndexPath() throws Exception;
}
