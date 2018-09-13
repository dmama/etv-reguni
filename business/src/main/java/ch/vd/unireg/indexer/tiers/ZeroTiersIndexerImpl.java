package ch.vd.unireg.indexer.tiers;

import java.util.Collection;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.Switchable;
import ch.vd.unireg.indexer.IndexerBatchException;
import ch.vd.unireg.indexer.IndexerException;
import ch.vd.unireg.tiers.Tiers;

public class ZeroTiersIndexerImpl implements GlobalTiersIndexer {

	@Override
	public int indexAllDatabase() throws IndexerException {
		return 0;
	}

	@Override
	public int indexAllDatabase(@NotNull Mode mode, int nbThreads, StatusManager statusManager) throws IndexerException {
		return 0;
	}

	@Override
	public void indexTiers(@NotNull List<Tiers> tiers, boolean removeBefore, boolean followDependents) throws IndexerBatchException {

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
	public Switchable onTheFlyIndexationSwitch() {
		return new Switchable() {
			@Override
			public void setEnabled(boolean enabled) {
			}

			@Override
			public boolean isEnabled() {
				return false;
			}
		};
	}

	@Override
	public void overwriteIndex() {
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
