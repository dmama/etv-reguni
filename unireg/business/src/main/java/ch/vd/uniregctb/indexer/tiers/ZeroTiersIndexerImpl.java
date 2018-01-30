package ch.vd.uniregctb.indexer.tiers;

import java.util.Collection;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.common.Switchable;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.TypeTiers;

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
	public int indexAllDatabase(@NotNull GlobalTiersIndexer.Mode mode, @NotNull Set<TypeTiers> typesTiers, int nbThreads, @Nullable StatusManager statusManager) throws IndexerException {
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
