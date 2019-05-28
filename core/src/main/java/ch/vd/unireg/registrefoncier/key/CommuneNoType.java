package ch.vd.unireg.registrefoncier.key;

/**
 * Le type de numéro de commune
 */
public enum CommuneNoType {
	/**
	 * Le numéro du registre foncier traditionnel.
	 */
	RF,
	/**
	 * Le numéro OFS de la commune (SIFISC-30558)
	 */
	OFS;

	/**
	 * Détecte le type de numéro d'identification d'une commune.
	 * @param numero un numéro de commune
	 * @return le type de numéro
	 */
	public static CommuneNoType detect(int numero) {
		if (numero < 1000) {
			// le nombre maximum en production est 338 pour Yvonand
			return RF;
		}
		else {
			// tous les numéros OFS de commune sont sur 4 positions
			return OFS;
		}
	}
}
