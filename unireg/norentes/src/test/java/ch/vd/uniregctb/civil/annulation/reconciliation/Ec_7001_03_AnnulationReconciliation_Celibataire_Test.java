package ch.vd.uniregctb.civil.annulation.reconciliation;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.annulation.reconciliation.Ec_7001_03_AnnulationReconciliation_Celibataire_Scenario;

/**
 * Teste l'événement annulation de réconciliation avec un habitant célibataire (cas d'erreur).
 * 
 * @author Pavel BLANCO
 *
 */
public class Ec_7001_03_AnnulationReconciliation_Celibataire_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_7001_03_AnnulationReconciliation_Celibataire_Scenario.NAME;
	}

}
