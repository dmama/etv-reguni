package ch.vd.uniregctb.norentes.civil.depart;

import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.norentes.common.NorentesTest;

/**
 * Test de l'événement de départ hors canton d'un habitant.
 *
 * @author xsipbo
 *
 */
@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessut-taches.xml"
})
public class Ec_19000_04_Depart_JIRA1262_Test extends NorentesTest {

	public Ec_19000_04_Depart_JIRA1262_Test() {
		setWantSynchroTache(true);
	}

	@Override
	protected String getScenarioName() {
		return Ec_19000_04_Depart_JIRA1262_Scenario.NAME;
	}

}
