package ch.vd.unireg.norentes.civil.changement.identification;

import ch.vd.unireg.norentes.common.NorentesTest;

public class Ec_48000_02_CorrectionIdentificationNonHabitant_Test extends NorentesTest {

	public Ec_48000_02_CorrectionIdentificationNonHabitant_Test() {
		setWantIndexationTiers(true);
	}

	@Override
	protected String getScenarioName() {
		return Ec_48000_02_CorrectionIdentificationNonHabitant_Scenario.NAME;
	}

}