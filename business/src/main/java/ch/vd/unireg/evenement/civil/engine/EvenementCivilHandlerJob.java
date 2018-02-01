package ch.vd.unireg.evenement.civil.engine;

import java.util.Map;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.evenement.civil.engine.ech.EvenementCivilEchRetryProcessor;
import ch.vd.unireg.scheduler.JobCategory;
import ch.vd.unireg.scheduler.JobDefinition;

public class EvenementCivilHandlerJob extends JobDefinition {

	public static final String NAME = "EvenementCivilHandlerJob";

	private EvenementCivilEchRetryProcessor processorEch;

	public EvenementCivilHandlerJob(int sortOrder, String description) {
		super(NAME, JobCategory.EVENTS, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {
		
		final StatusManager status = getStatusManager();

		// Validation/Traitement des événements civils
		status.setMessage("Traitement des événements e-CH...");
		processorEch.retraiteEvenements(status);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProcessorEch(EvenementCivilEchRetryProcessor processorEch) {
		this.processorEch = processorEch;
	}
}
