package ch.vd.uniregctb.jmx;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.common.TimeHelper;
import ch.vd.uniregctb.extraction.ExtractionJob;
import ch.vd.uniregctb.extraction.ExtractionService;

/**
 * Implémentation du bean JMX de monitoring des extractions asynchrones
 */
@ManagedResource
public class ExtractionServiceJmxBeanImpl implements ExtractionServiceJmxBean {

	private ExtractionService extractionService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExtractionService(ExtractionService extractionService) {
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
		final List<ExtractionJob> extraction = extractionService.getQueueContent(null);
		final List<String> descriptions;
		if (extraction.size() > 0) {
			descriptions = new ArrayList<String>(extraction.size());
			for (ExtractionJob info : extraction) {
				descriptions.add(info.toString());
			}
		}
		else {
			descriptions = Collections.emptyList();
		}
		return descriptions;

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
		final List<ExtractionJob> liste = extractionService.getExtractionsEnCours(null);
		final List<String> strs = new ArrayList<String>(liste.size());
		for (ExtractionJob enCours : liste) {
			if (enCours != null) {
				final Long duree = enCours.getDuration();
				if (duree != null) {
					final StringBuilder progress = new StringBuilder();
					final String msg = enCours.getRunningMessage();
					final Integer percent = enCours.getPercentProgression();
					if (StringUtils.isNotBlank(msg)) {
						progress.append(' ').append(msg);
					}
					if (percent != null) {
						progress.append(" (").append(percent).append("%)");
					}
					strs.add(String.format("%s, en cours depuis %s.%s", enCours, TimeHelper.formatDuree(duree), progress.toString()));
				}
				else {
					// devrait être rare (si je job est en cours, il devrait avoir une durée...), mais
					// on peut avoir le currentJob setté alors que le onStart() n'a pas encore été appelé, donc...
					strs.add(enCours.toString());
				}
			}
		}
		return strs;
	}
}
