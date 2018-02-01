package ch.vd.unireg.adresse;

public interface AdresseFiscaleEtrangere extends AdresseFiscale {

	/**
	 * Données complémentaires quant au lieu, comme par exemple la région, la province, l'état fédéral ou le quartier.
	 * Longueur selon eCH-0010 : 40
	 */
	String getComplementLocalite();

	/**
	 * Lieu de l'adresse.
	 * Longueur selon eCH-0010 : 40
	 */
	String getNumeroPostalLocalite();

	/**
	 * Abréviation ISO 3166-1 du pays dans lequel se trouve le lieu faisant partie de l'adresse postale.
	 * Longueur selon eCH-0010 : 2
	 */
	Integer getNumeroOfsPays();
}
