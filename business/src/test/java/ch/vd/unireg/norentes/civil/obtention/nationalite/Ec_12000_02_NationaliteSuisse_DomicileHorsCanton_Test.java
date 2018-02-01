package ch.vd.unireg.norentes.civil.obtention.nationalite;

import org.springframework.test.context.ContextConfiguration;

import ch.vd.unireg.norentes.common.NorentesTest;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessut-taches.xml"
})
public class Ec_12000_02_NationaliteSuisse_DomicileHorsCanton_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_12000_02_NationaliteSuisse_DomicileHorsCanton_Scenario.NAME;
	}

}