package ch.vd.uniregctb.civil.arrivee;

import org.springframework.test.context.ContextConfiguration;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.arrivee.Ec_18000_08_Depart_HS_Arrivee_HC_Meme_Periode_Scenario;

@ContextConfiguration(locations = {
		"classpath:ut/unireg-businessut-taches.xml"
})
public class Ec_18000_08_Depart_HS_Arrivee_HC_Meme_Periode_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_18000_08_Depart_HS_Arrivee_HC_Meme_Periode_Scenario.NAME;
	}

}
