package ch.vd.uniregctb.norentes.civil.arrivee;

import ch.vd.uniregctb.norentes.common.NorentesTest;

public class Ec_18000_17_Arrivee_JIRA1677_Test extends NorentesTest {

	public Ec_18000_17_Arrivee_JIRA1677_Test() {
		setWantIndexationTiers(true);
	}

	@Override
	protected String getScenarioName() {
		return Ec_18000_17_Arrivee_JIRA1677_Scenario.NAME;
	}
}