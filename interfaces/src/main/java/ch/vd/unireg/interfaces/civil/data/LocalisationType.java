package ch.vd.unireg.interfaces.civil.data;

/**
 * Distingue les trois principales localisations géographiques utilisées dans les données civiles.
 */
public enum LocalisationType {
	/**
	 * Le lieu est dans le canton de Vaud.
	 */
	CANTON_VD,
	/**
	 * Le lieu est dans un autre canton que celui de Vaud.
	 */
	HORS_CANTON,
	/**
	 * Le lieu est dans un pays étranger.
	 */
	HORS_SUISSE
}
