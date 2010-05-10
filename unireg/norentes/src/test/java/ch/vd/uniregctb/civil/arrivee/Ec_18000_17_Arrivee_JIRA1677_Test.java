package ch.vd.uniregctb.civil.arrivee;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.arrivee.Ec_18000_17_Arrivee_JIRA1677_Scenario;

public class Ec_18000_17_Arrivee_JIRA1677_Test extends NorentesTest {

	public Ec_18000_17_Arrivee_JIRA1677_Test() {
		setWantIndexation(true);
	}

	@Override
	protected String getScenarioName() {
		return Ec_18000_17_Arrivee_JIRA1677_Scenario.NAME;
	}
}