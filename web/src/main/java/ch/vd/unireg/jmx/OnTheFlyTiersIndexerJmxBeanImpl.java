package ch.vd.unireg.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;

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
}
