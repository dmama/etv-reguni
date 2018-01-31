package ch.vd.uniregctb.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;

@ManagedResource
public class OnTheFlyTiersIndexerJmxBeanImpl implements OnTheFlyTiersIndexerJmxBean {

	private GlobalTiersIndexer globalTiersIndexer;

	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		this.globalTiersIndexer = globalTiersIndexer;
	}

	@ManagedAttribute(description = "Nombre de tiers en attente d'indexation asynchrone.")
	@Override
	public int getQueueSize() {
		return globalTiersIndexer.getOnTheFlyQueueSize();
	}

	@ManagedAttribute(description = "Nombre de threads d'indexation actuellement activés.")
	@Override
	public int getThreadNumber() {
		return globalTiersIndexer.getOnTheFlyThreadNumber();
	}
}
