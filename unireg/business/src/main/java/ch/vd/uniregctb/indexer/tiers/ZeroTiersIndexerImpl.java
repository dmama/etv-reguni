package ch.vd.uniregctb.indexer.tiers;

import java.util.List;

import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.indexer.GlobalIndexInterface;
import ch.vd.uniregctb.indexer.IndexerBatchException;
import ch.vd.uniregctb.indexer.IndexerException;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersDAO;
import ch.vd.uniregctb.tiers.TiersService;

public class ZeroTiersIndexerImpl implements GlobalTiersIndexer {

	public AdresseService getAdresseService() {
		return null;
	}

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

	public void indexTiers(long id) throws IndexerException {
	}

	public void indexTiers(Tiers tiers) throws IndexerException {
	}

	public void indexTiers(Tiers tiers, boolean removeBefore) throws IndexerException {
	}

	public void indexTiers(Tiers tiers, boolean removeBefore, boolean followDependents) throws IndexerException {
	}

	public boolean isOnTheFlyIndexation() {
		return false;
	}

	public boolean isThrowOnTheFlyException() {
		return false;
	}

	public void overwriteIndex() {
	}

	public void removeEntity(Long id, String type) {
	}

	public void setAdresseService(AdresseService adresseService) {
	}

	public void setGlobalIndex(GlobalIndexInterface globalIndex) {
	}

	public void setOnTheFlyIndexation(boolean onTheFlyIndexation) {
	}

	public void setServiceCivilService(ServiceCivilService civilService) {
	}

	public void setThrowOnTheFlyException(boolean want) {
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
	}

	public void setTiersService(TiersService tiersService) {
	}

	public void indexTiers(List<Tiers> tiers, boolean removeBefore, boolean followDependents) throws IndexerBatchException {
	}
}
