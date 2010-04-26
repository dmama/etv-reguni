package ch.vd.uniregctb.civil.separation;

import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.separation.Ec_6000_08_Separation_JIRA1057_Scenario;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessut-taches.xml"
})
public class Ec_6000_08_Separation_JIRA1057_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_6000_08_Separation_JIRA1057_Scenario.NAME;
	}
}
