package ch.vd.uniregctb.civil.fin.permis;

import ch.vd.uniregctb.civil.common.NorentesTest;
import ch.vd.uniregctb.norentes.civil.fin.permis.Ec_16010_02_FinPermis_PermisCSansNationaliteSuisse_Scenario;

/**
 * Teste l'événement Fin de Permis pour le cas d'un habitant avec permis C n'ayant pas obtenu la nationalité suisse.
 * 
 * @author Pavel BLANCO
 *
 */
public class Ec_16010_02_FinPermis_PermisCSansNationaliteSuisse_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_16010_02_FinPermis_PermisCSansNationaliteSuisse_Scenario.NAME;
	}

}
