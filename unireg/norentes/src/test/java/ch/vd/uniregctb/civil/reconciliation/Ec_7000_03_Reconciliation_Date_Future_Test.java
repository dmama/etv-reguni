package ch.vd.uniregctb.civil.reconciliation;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.reconciliation.Ec_7000_03_Reconciliation_Date_Future_Scenario;

/**
 * Scénario de réconciliation avec une date dans le futur
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_7000_03_Reconciliation_Date_Future_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_7000_03_Reconciliation_Date_Future_Scenario.NAME;
	}
}
