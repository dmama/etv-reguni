package ch.vd.unireg.norentes.civil.changement.dateNaissance;

import ch.vd.unireg.norentes.common.NorentesTest;

public class Ec_41010_01_CorrectionDateNaissance_Test extends NorentesTest {

	public Ec_41010_01_CorrectionDateNaissance_Test() {
		setWantIndexationTiers(true);
	}

	@Override
	protected String getScenarioName() {
		return Ec_41010_01_CorrectionDateNaissance_Scenario.NAME;
	}

}
