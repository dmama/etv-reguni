package ch.vd.uniregctb.checker;

import java.util.List;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersSearcher;
import ch.vd.uniregctb.indexer.tiers.TiersIndexedData;
import ch.vd.uniregctb.tiers.TiersCriteria;

public class TiersSearcherChecker {

	private GlobalTiersSearcher searcher;
	private String details;

	public Status getStatus() {
		try {
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(0L); // ce tiers n'existe pas et ne peut pas exister
			final List<TiersIndexedData> list = searcher.search(criteria);
			Assert.isEqual(0, list.size());
			details = null;
			return Status.OK;
		}
		catch (Exception e) {
			details = ExceptionUtils.extractCallStack(e);
			return Status.KO;
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public String getStatusDetails() {
		return details;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSearcher(GlobalTiersSearcher searcher) {
		this.searcher = searcher;
	}
}
