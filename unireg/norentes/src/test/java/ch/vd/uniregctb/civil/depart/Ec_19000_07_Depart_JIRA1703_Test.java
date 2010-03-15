package ch.vd.uniregctb.civil.depart;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.depart.Ec_19000_07_Depart_JIRA1703_Scenario;

/**
 * Test de l'événement de départ hors suisse des membres d'un couple dont l'adresse
 * fiscale est surchargée de manière permanente
 */
public class Ec_19000_07_Depart_JIRA1703_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_19000_07_Depart_JIRA1703_Scenario.NAME;
	}

}