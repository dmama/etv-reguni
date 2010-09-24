package ch.vd.uniregctb.civil.veuvage;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.veuvage.Ec_10000_06_Veuvage_VeuvageHabitantMarieAvecNonHabitant_Scenario;

/**
 * Test du scénario de veuvage d'un habitant marié avec un non-habitant (inconnu au civil, donc marié seul au civil)
 */
public class Ec_10000_06_Veuvage_VeuvageHabitantMarieAvecNonHabitant_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_10000_06_Veuvage_VeuvageHabitantMarieAvecNonHabitant_Scenario.NAME;
	}

}