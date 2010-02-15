package ch.vd.uniregctb.civil.reconciliation;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.reconciliation.Ec_7000_04_Reconciliation_Non_Separes_Scenario;

/**
 * Scénario de réconciliation avec des habitants non séparés
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_7000_04_Reconciliation_Non_Separes_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_7000_04_Reconciliation_Non_Separes_Scenario.NAME;
	}
}
