package ch.vd.uniregctb.indexer.tiers;

import java.util.Collection;

import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.indexer.IndexerException;

public class ZeroTiersIndexerImpl implements GlobalTiersIndexer {

	@Override
	public int indexAllDatabase() throws IndexerException {
		return 0;
	}

	@Override
	public int indexAllDatabase(StatusManager statusManager, int nbThreads, Mode mode, boolean prefetchPMs) throws IndexerException {
		return 0;
	}

	@Override
	public void schedule(long id) {
	}

	@Override
	public void schedule(Collection<Long> ids) {
	}

	@Override
	public void sync() {
	}

	@Override
	public boolean isOnTheFlyIndexation() {
		return false;
	}

	@Override
	public void overwriteIndex() {
	}

	@Override
	public void setOnTheFlyIndexation(boolean onTheFlyIndexation) {
	}

	@Override
	public int getOnTheFlyQueueSize() {
		return 0;
	}

	@Override
	public int getOnTheFlyThreadNumber() {
		return 0;
	}
}
