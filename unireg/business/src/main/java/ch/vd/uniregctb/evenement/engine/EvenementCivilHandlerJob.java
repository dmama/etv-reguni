package ch.vd.uniregctb.evenement.engine;

import java.util.HashMap;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class EvenementCivilHandlerJob extends JobDefinition {

	public static String NAME = "EvenementCivilHandlerJob";
	private static final String CATEGORIE = "Events";

	EvenementCivilProcessor processor = null;

	public EvenementCivilHandlerJob(int sortOrder) {
		super(NAME, CATEGORIE, sortOrder, "Traitement (ou retraitement) des événements civils");
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final StatusManager status = getStatusManager();

		// Validation/Traitement des événements civils
		status.setMessage("traitement des événements...");
		processor.traiteEvenementsCivilsRegroupes(status);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilProcessor(EvenementCivilProcessor proc) {
		this.processor = proc;
	}
}
