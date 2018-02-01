package ch.vd.uniregctb.norentes.civil.obtention.permis;

import ch.vd.uniregctb.norentes.common.NorentesTest;

/**
 * Obtention d'un permis C sur un sourcier non-résident sur le canton
 * (vérification du type d'autorité fiscale du nouveau for principal ordinaire de domicile),
 * issu du cas Jira UNIREG-1891
 */
public class Ec_16000_03_ObtentionPermis_NonResident_Test extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_16000_03_ObtentionPermis_NonResident_Scenario.NAME;
	}

}
