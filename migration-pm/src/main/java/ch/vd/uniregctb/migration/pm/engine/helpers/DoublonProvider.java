package ch.vd.uniregctb.migration.pm.engine.helpers;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;

/**
 * Interface du service d'identification des doublons (= des entreprises qui doivent être migrées
 * en tenant compte du fait qu'elles représentent un doublon)
 */
public interface DoublonProvider {

	/**
	 * @param entreprise entreprise RegPM à tester
	 * @return <code>true</code> si l'entreprise doit être migrée comme un doublon
	 */
	boolean isDoublon(RegpmEntreprise entreprise);
}
