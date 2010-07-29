package ch.vd.uniregctb.civil.reconciliation;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.reconciliation.Ec_7000_05_Reconciliation_Non_Assujettis_Scenario;

/**
 * Scénario de réconciliation avec des habitants non assujettis
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_7000_05_Reconciliation_Non_Assujettis_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_7000_05_Reconciliation_Non_Assujettis_Scenario.NAME;
	}
}
