package ch.vd.uniregctb.jmx;

import java.util.List;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.extraction.ExtractionServiceMonitoring;

/**
 * Implémentation du bean JMX de monitoring des extractions asynchrones
 */
@ManagedResource
public class ExtractionServiceJmxBeanImpl implements ExtractionServiceJmxBean {

	private ExtractionServiceMonitoring extractionService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExtractionService(ExtractionServiceMonitoring extractionService) {
		this.extractionService = extractionService;
	}

	@ManagedAttribute
	@Override
	public int getQueueSize() {
		return extractionService.getQueueSize();
	}

	@ManagedAttribute
	@Override
	public int getExecutorNumber() {
		return extractionService.getNbExecutors();
	}

	@ManagedAttribute
	@Override
	public List<String> getQueueContent() {
		return extractionService.getQueueContent();
	}

	@ManagedAttribute
	@Override
	public int getNumberOfExecutedQueries() {
		return extractionService.getNbExecutedQueries();
	}

	@ManagedAttribute
	@Override
	public int getExpirationInDays() {
		return extractionService.getDelaiExpirationEnJours();
	}

	@ManagedAttribute
	@Override
	public List<String> getRunningJobs() {
		return extractionService.getExtractionsEnCours();
	}
}
