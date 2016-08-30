package ch.vd.uniregctb.migration.pm.engine;

import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;

/**
 * Service capable de maintenir l'information qu'une entreprise est active ou pas
 * au moment de la migration
 */
public interface ActivityManager {

	/**
	 * @param entreprise entreprise de RegPM
	 * @return si oui ou non l'entreprise est considérée comme active au moment de la migration
	 */
	boolean isActive(RegpmEntreprise entreprise);
}
