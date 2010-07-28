package ch.vd.uniregctb.tiers;

/**
 * Interface qui spécifie que l'entité hibernate courante est possédée par un tiers (de manière directe ou transiente).
 */
public interface TiersSubEntity {

	/**
	 * @return le tiers qui possède l'entité hibernate courante.
	 */
	Tiers getTiersParent();
}
