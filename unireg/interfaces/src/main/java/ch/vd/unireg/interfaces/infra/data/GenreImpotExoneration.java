package ch.vd.unireg.interfaces.infra.data;

/**
 * @author Raphaël Marmier, 2017-03-10, <raphael.marmier@vd.ch>
 */
public enum GenreImpotExoneration {
	/**
	 * Impôt sur le bénéfice et le capital
	 */
	IBC,

	/**
	 * Impôt complémentaire sur immeuble
	 */
	ICI,

	/**
	 * Impôt foncier
	 */
	IFONC,

	/**
	 * Autre type, pour gérer la compatibilité en cas d'apparition dans le système source (FIDOR, en l'occurrence), d'un nouveau genre d'impôt susceptible de porter une exonération
	 */
	AUTRE
}
