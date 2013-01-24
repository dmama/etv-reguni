package ch.vd.uniregctb.indexer.tiers;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.TiersCriteria;
import ch.vd.uniregctb.tiers.TiersFilter;

public class ZeroTiersSearcherImpl implements GlobalTiersSearcher {

	@Override
	public List<TiersIndexedData> search(TiersCriteria criteria) throws IndexerException {
		return Collections.emptyList();
	}

	@Override
	public TopList<TiersIndexedData> searchTop(TiersCriteria criteria, int max) throws IndexerException {
		return new TopList<TiersIndexedData>();
	}

	@Override
	public TopList<TiersIndexedData> searchTop(String keywords, TiersFilter filter, int max) throws IndexerException {
		return new TopList<TiersIndexedData>();
	}

	@Override
	public boolean exists(Long numero) throws IndexerException {
		return false;
	}

	@Override
	public TiersIndexedData get(Long numero) throws IndexerException {
		return null;
	}

	@Override
	public Set<Long> getAllIds() {
		return Collections.emptySet();
	}

	@Override
	public void checkCoherenceIndex(Set<Long> existingIds, StatusManager statusManager, CheckCallback callback) {
	}

	@Override
	public int getApproxDocCount() {
		return 0;
	}

	@Override
	public int getExactDocCount() {
		return 0;
	}
}
