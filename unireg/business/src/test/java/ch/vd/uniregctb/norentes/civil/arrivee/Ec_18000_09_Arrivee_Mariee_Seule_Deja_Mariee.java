package ch.vd.uniregctb.norentes.civil.arrivee;

import ch.vd.uniregctb.norentes.common.NorentesTest;

/**
 * Cette classe teste l'arrivée l'une personne mariée seule, alors qu'elle existe déjà dans le registre comme composant d'un ménage actif
 * (cas réel observé en production, voir l'événement civil n°522)
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Ec_18000_09_Arrivee_Mariee_Seule_Deja_Mariee extends NorentesTest {

	@Override
	protected String getScenarioName() {
		return Ec_18000_09_Arrivee_Mariee_Seule_Deja_Mariee_Scenario.NAME;
	}
}
