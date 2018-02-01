package ch.vd.unireg.checker;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.indexer.tiers.GlobalTiersSearcher;
import ch.vd.unireg.indexer.tiers.TiersIndexedData;
import ch.vd.unireg.tiers.TiersCriteria;

public class TiersSearcherChecker implements StatusChecker {

	private GlobalTiersSearcher searcher;

	@NotNull
	@Override
	public String getName() {
		return "globalTiersSearcher";
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
		try {
			final TiersCriteria criteria = new TiersCriteria();
			criteria.setNumero(0L); // ce tiers n'existe pas et ne peut pas exister
			final List<TiersIndexedData> list = searcher.search(criteria);
			if (!list.isEmpty()) {
				throw new CheckerException("Données incohérentes retournées");
			}
		}
		catch (CheckerException e) {
			throw e;
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setSearcher(GlobalTiersSearcher searcher) {
		this.searcher = searcher;
	}
}
