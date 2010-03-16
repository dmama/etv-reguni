package ch.vd.uniregctb.civil.annulation.mariage;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.annulation.mariage.Ec_4001_05_AnnulationMariage_Couple_Scenario;

/**
 * Test de l'événement annulation de mariage du cas JIRA UNIREG-1086.
 * 
 * @author Pavel BLANCO
 *
 */
public class Ec_4001_05_AnnulationMariage_Couple_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_4001_05_AnnulationMariage_Couple_Scenario.NAME;
	}

}
