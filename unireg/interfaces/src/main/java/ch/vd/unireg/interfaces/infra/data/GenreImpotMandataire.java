package ch.vd.unireg.interfaces.infra.data;

/**
 * Genre d'impôt utilisable dans les mandats spéciaux
 */
public interface GenreImpotMandataire {

	/**
	 * @return un code
	 */
	String getCode();

	/**
	 * @return un libellé
	 */
	String getLibelle();
}
