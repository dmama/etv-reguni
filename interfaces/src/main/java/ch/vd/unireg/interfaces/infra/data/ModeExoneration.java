package ch.vd.unireg.interfaces.infra.data;

/**
 * @author Raphaël Marmier, 2017-03-10, <raphael.marmier@vd.ch>
 */
public enum ModeExoneration {

	/**
	 * Exonération totale : aucun assujettissement, aucune déclaration ni décision de taxation
	 */
	TOTALE,

	/**
	 * Exonération de fait : entité assujettie mais taxée à zéro
	 */
	DE_FAIT,

	/**
	 * Autre type, pour gérer la compatibilité en cas d'apparition dans le système source (FIDOR, en l'occurrence), d'un nouveau mode...
	 */
	AUTRE
}
