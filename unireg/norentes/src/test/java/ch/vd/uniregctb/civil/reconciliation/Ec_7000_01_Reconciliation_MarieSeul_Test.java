package ch.vd.uniregctb.civil.reconciliation;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.reconciliation.Ec_7000_01_Reconciliation_MarieSeul_Scenario;

/**
 * Test de l'événement réconciliation pour le cas d'une personne mariée seule.
 * 
 * @author Pavel BLANCO
 *
 */
public class Ec_7000_01_Reconciliation_MarieSeul_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_7000_01_Reconciliation_MarieSeul_Scenario.NAME;
	}

}
