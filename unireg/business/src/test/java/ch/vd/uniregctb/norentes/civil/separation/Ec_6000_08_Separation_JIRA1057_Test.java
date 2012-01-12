package ch.vd.uniregctb.norentes.civil.separation;

import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.norentes.common.NorentesTest;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessut-taches.xml"
})
public class Ec_6000_08_Separation_JIRA1057_Test extends NorentesTest {

	public Ec_6000_08_Separation_JIRA1057_Test() {
		setWantSynchroTache(true);
	}

	@Override
	protected String getScenarioName() {
		return Ec_6000_08_Separation_JIRA1057_Scenario.NAME;
	}
}
