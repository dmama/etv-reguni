package ch.vd.unireg.type;

/**
 * Les différents types possibles de rapprochement entre un tiers RF et un contribuable Unireg
 */
public enum TypeRapprochementRF {

	/**
	 * Rapprochement suite à une identification automatique parfaite
	 */
	AUTO,

	/**
	 * Rapprochement suite à une identification automatique avec plusieurs candidats dont un explicitement donné par le RF
	 */
	AUTO_MULTIPLE,

	/**
	 * Rapprochement suite à une identification manuelle
	 */
	MANUEL
}
