package ch.vd.uniregctb.civil.changement.identification;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.changement.identification.Ec_48000_01_CorrectionIdentificationHabitant_Scenario;

public class Ec_48000_01_CorrectionIdentificationHabitant_Test extends NorentesTest {

	public Ec_48000_01_CorrectionIdentificationHabitant_Test() {
		setWantIndexation(true);
	}

	@Override
	protected String getScenarioName() {
		return Ec_48000_01_CorrectionIdentificationHabitant_Scenario.NAME;
	}

}