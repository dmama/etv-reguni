package ch.vd.uniregctb.civil.mariage;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.mariage.Ec_4000_09_Mariage_JIRA1481_Scenario;

/**
 * Test du scénario de mariage d'un couple, dont seul le conjoint est 
 * assujetti et la date de mariage antérieure à celle de son for.
 * 
 * @author Pavel BLANCO
 *
 */
public class Ec_4000_09_Mariage_JIRA1481_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_4000_09_Mariage_JIRA1481_Scenario.NAME;
	}

}
