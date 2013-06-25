package ch.vd.uniregctb.evenement.civil.engine;

import java.util.Map;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchRetryProcessor;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class EvenementCivilHandlerJob extends JobDefinition {

	public static final String NAME = "EvenementCivilHandlerJob";
	private static final String CATEGORIE = "Events";
	
	private EvenementCivilEchRetryProcessor processorEch;

	public EvenementCivilHandlerJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
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
