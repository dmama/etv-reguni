package ch.vd.unireg.jmx;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

/**
 * Interface du bean JMX de monitoring de l'indexation on-the-fly
 */
@ManagedResource
public interface OnTheFlyTiersIndexerJmxBean {

	@ManagedAttribute(description = "Nombre de tiers en attente d'indexation asynchrone.")
	int getQueueSize();
}
