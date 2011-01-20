package ch.vd.uniregctb.indexer.tiers;

import java.util.Collection;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.IndexerException;

public class ZeroTiersIndexerImpl implements GlobalTiersIndexer {

	public int indexAllDatabase() throws IndexerException {
		return 0;
	}

	public int indexAllDatabase(boolean assertSameNumber, StatusManager statusManager) throws IndexerException {
		return 0;
	}

	public int indexAllDatabaseAsync(StatusManager statusManager, int nbThreads, Mode mode, boolean prefetchIndividus)
			throws IndexerException {
		return 0;
	}

	public void schedule(long id) {
	}

	public void schedule(Collection<Long> ids) {
	}

	public void sync() {
	}

	public boolean isOnTheFlyIndexation() {
		return false;
	}

	public void overwriteIndex() {
	}

	public void setOnTheFlyIndexation(boolean onTheFlyIndexation) {
	}
}
