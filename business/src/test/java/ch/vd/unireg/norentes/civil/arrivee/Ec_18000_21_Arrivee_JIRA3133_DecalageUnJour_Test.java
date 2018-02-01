package ch.vd.unireg.norentes.civil.arrivee;

import ch.vd.unireg.norentes.common.NorentesTest;

public class Ec_18000_21_Arrivee_JIRA3133_DecalageUnJour_Test extends NorentesTest {

	public Ec_18000_21_Arrivee_JIRA3133_DecalageUnJour_Test() {
		setWantIndexationTiers(true);
	}

	@Override
	protected String getScenarioName() {
		return Ec_18000_21_Arrivee_JIRA3133_DecalageUnJour_Scenario.NAME;
	}
}