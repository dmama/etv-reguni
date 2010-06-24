package ch.vd.uniregctb.civil.obtention.nationalite;

import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.obtention.nationalite.Ec_12000_02_NationaliteSuisse_DomicileHorsCanton_Scenario;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessut-taches.xml"
})
public class Ec_12000_02_NationaliteSuisse_DomicileHorsCanton_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_12000_02_NationaliteSuisse_DomicileHorsCanton_Scenario.NAME;
	}

}