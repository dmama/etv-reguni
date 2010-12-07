package ch.vd.uniregctb.indexer.tiers;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.tiers.TiersCriteria;

public class ZeroTiersSearcherImpl implements GlobalTiersSearcher {

	public List<TiersIndexedData> search(TiersCriteria criteria) throws IndexerException {
		return Collections.emptyList();
	}

	public TopList<TiersIndexedData> searchTop(String keywords, int max) throws IndexerException {
		return new TopList<TiersIndexedData>();
	}

	public boolean exists(Long numero) throws IndexerException {
		return false;
	}

	public TiersIndexedData get(Long numero) throws IndexerException {
		return null;
	}

	public Set<Long> getAllIds() {
		return Collections.emptySet();
	}

	public void checkCoherenceIndex(Set<Long> existingIds, StatusManager statusManager, CheckCallback callback) {
	}

	public int getApproxDocCount() {
		return 0;
	}

	public int getExactDocCount() {
		return 0;
	}
}
