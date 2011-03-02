package ch.vd.uniregctb.jmx;

import java.util.List;

import org.apache.cxf.management.annotation.ManagedAttribute;

/**
 * Interface du bean JMX de monitoring du service d'extractions asynchrones
 */
public interface ExtractionServiceJmxBean {

	@ManagedAttribute(description = "Nombre de demandes d'extractions actuellement en attentes")
	int getQueueSize();

	@ManagedAttribute(description = "Nombre de demandes d'extractions traitables en parallèle")
	int getExecutorNumber();

	@ManagedAttribute(description = "Liste des demandes en attente")
	List<String> getQueueContent();

	@ManagedAttribute(description = "Nombre de demandes d'extractions asynchrones exécutées depuis le démarrage du service")
	int getNumberOfExecutedQueries();

	@ManagedAttribute(description = "Nombre de jours pendant lesquels un résultat d'extraction asynchrone est conservé")
	int getExpirationInDays();

	@ManagedAttribute(description = "Liste des demandes en cours de traitement")
	List<String> getRunningJobs();
}
