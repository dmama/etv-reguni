package ch.vd.uniregctb.evenement.civil.engine;

import java.util.Map;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class EvenementCivilHandlerJob extends JobDefinition {

	public static String NAME = "EvenementCivilHandlerJob";
	private static final String CATEGORIE = "Events";

	private EvenementCivilProcessor processor = null;

	public EvenementCivilHandlerJob(int sortOrder, String description) {
		super(NAME, CATEGORIE, sortOrder, description);
	}

	@Override
	protected void doExecute(Map<String, Object> params) throws Exception {

		final StatusManager status = getStatusManager();

		// Validation/Traitement des événements civils
		status.setMessage("traitement des événements...");
		processor.traiteEvenementsCivils(status);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilProcessor(EvenementCivilProcessor proc) {
		this.processor = proc;
	}
}
