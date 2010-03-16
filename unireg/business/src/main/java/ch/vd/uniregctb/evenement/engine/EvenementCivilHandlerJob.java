package ch.vd.uniregctb.evenement.engine;

import java.util.HashMap;

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
		/* 1 - Regroupement des nouveaux événements unitaires */
		regrouper.regroupeTousEvenementsNonTraites();

		/* 2- Validation/Traitement des événements regroupés */
		processor.traiteEvenementsCivilsRegroupes();
	}

	/**
	 * Positionne l'implémentation du moteur de règles.
	 *
	 */
	public void setEvenementCivilProcessor(EvenementCivilProcessor proc) {
		this.processor = proc;
	}

	/**
	 * Positionne l'implémentation de IEvenementCivilUnitaireReader.
	 *
	 * @param reader
	 *            l'implémentation de IEvenementCivilUnitaireReader.
	 */
	public void setRegrouper(EvenementCivilRegrouper reader) {
		this.regrouper = reader;
	}

}
