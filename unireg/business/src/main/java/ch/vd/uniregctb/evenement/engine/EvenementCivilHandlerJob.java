package ch.vd.uniregctb.evenement.engine;

import java.util.HashMap;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.scheduler.JobDefinition;

public class EvenementCivilHandlerJob extends JobDefinition {

	public static String NAME = "EvenementCivilHandlerJob";
	private static final String CATEGORIE = "Events";

	EvenementCivilRegrouper regrouper = null;
	EvenementCivilProcessor processor = null;

	public EvenementCivilHandlerJob(int sortOrder) {
		super(NAME, CATEGORIE, sortOrder, "Regroupement et traitement des Evénements civils");
	}

	@Override
	protected void doExecute(HashMap<String, Object> params) throws Exception {

		final StatusManager status = getStatusManager();

		/* 1 - Regroupement des nouveaux événements unitaires */
		status.setMessage("regroupement des événements...");
		regrouper.regroupeTousEvenementsNonTraites(status);

		/* 2- Validation/Traitement des événements regroupés */
		status.setMessage("traitement des événements...");
		processor.traiteEvenementsCivilsRegroupes(status);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvenementCivilProcessor(EvenementCivilProcessor proc) {
		this.processor = proc;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRegrouper(EvenementCivilRegrouper reader) {
		this.regrouper = reader;
	}

}
