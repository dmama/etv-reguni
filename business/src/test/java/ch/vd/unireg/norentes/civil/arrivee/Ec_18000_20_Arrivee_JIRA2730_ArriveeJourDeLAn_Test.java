package ch.vd.unireg.norentes.civil.arrivee;

import ch.vd.unireg.norentes.common.NorentesTest;

public class Ec_18000_20_Arrivee_JIRA2730_ArriveeJourDeLAn_Test extends NorentesTest {

	public Ec_18000_20_Arrivee_JIRA2730_ArriveeJourDeLAn_Test() {
		setWantIndexationTiers(true);
	}

	@Override
	protected String getScenarioName() {
		return Ec_18000_20_Arrivee_JIRA2730_ArriveeJourDeLAn_Scenario.NAME;
	}
}