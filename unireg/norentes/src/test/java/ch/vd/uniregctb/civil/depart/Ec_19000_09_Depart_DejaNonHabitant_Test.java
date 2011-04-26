package ch.vd.uniregctb.civil.depart;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.depart.Ec_19000_09_Depart_DejaNonHabitant_Scenario;

/**
 * Test de l'événement de départ hors canton alors que la personne est déjà non-habitante (le flag peut être erroné)
 */
public class Ec_19000_09_Depart_DejaNonHabitant_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_19000_09_Depart_DejaNonHabitant_Scenario.NAME;
	}

}