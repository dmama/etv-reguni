package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.shared.batchtemplate.StatusManager;
import ch.vd.uniregctb.indexer.IndexerException;

public class MockTiersIndexer implements GlobalTiersIndexer {

	public final List<Long> scheduled = new ArrayList<>();

	@Override
	public void overwriteIndex() {
		throw new NotImplementedException();
	}

	@Override
	public void schedule(long id) {
		scheduled.add(id);
	}

	@Override
	public void schedule(Collection<Long> ids) {
		scheduled.addAll(ids);
	}

	@Override
	public void sync() {
	}

	@Override
	public int indexAllDatabase() throws IndexerException {
		throw new NotImplementedException();
	}

	@Override
	public int indexAllDatabase(StatusManager statusManager, int nbThreads, Mode mode) throws IndexerException {
		throw new NotImplementedException();
	}

	@Override
	public boolean isOnTheFlyIndexation() {
		throw new NotImplementedException();
	}

	@Override
	public void setOnTheFlyIndexation(boolean onTheFlyIndexation) {
		throw new NotImplementedException();
	}

	@Override
	public int getOnTheFlyQueueSize() {
		return scheduled.size();
	}

	@Override
	public int getOnTheFlyThreadNumber() {
		return 0;
	}
}
