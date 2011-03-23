package ch.vd.uniregctb.indexer.tiers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.IndexerException;

public class MockTiersIndexer implements GlobalTiersIndexer {

	public List<Long> scheduled = new ArrayList<Long>();

	public void overwriteIndex() {
		throw new NotImplementedException();
	}

	public void schedule(long id) {
		scheduled.add(id);
	}

	public void schedule(Collection<Long> ids) {
		scheduled.addAll(ids);
	}

	public void sync() {
	}

	public int indexAllDatabase() throws IndexerException {
		throw new NotImplementedException();
	}

	public int indexAllDatabase(boolean assertSameNumber, StatusManager statusManager) throws IndexerException {
		throw new NotImplementedException();
	}

	public int indexAllDatabaseAsync(StatusManager statusManager, int nbThreads, Mode mode, boolean prefetchIndividus) throws IndexerException {
		throw new NotImplementedException();
	}

	public boolean isOnTheFlyIndexation() {
		throw new NotImplementedException();
	}

	public void setOnTheFlyIndexation(boolean onTheFlyIndexation) {
		throw new NotImplementedException();
	}
}
