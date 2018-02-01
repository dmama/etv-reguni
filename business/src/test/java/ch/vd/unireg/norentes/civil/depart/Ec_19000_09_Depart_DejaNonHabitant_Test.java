package ch.vd.uniregctb.norentes.civil.depart;

import ch.vd.uniregctb.norentes.common.NorentesTest;

/**
 * Test de l'événement de départ hors canton alors que la personne est déjà non-habitante (le flag peut être erroné)
 */
public class Ec_19000_09_Depart_DejaNonHabitant_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_19000_09_Depart_DejaNonHabitant_Scenario.NAME;
	}

}