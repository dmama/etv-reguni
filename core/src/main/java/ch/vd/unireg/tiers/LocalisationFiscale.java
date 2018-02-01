package ch.vd.unireg.tiers;

import ch.vd.unireg.type.TypeAutoriteFiscale;

/**
 * Interface qui définit un accès à une localisation (= type autorité fiscale + numéro OFS)
 */
public interface LocalisationFiscale {

	/**
	 * @return le type d'autorité fiscale
	 */
	TypeAutoriteFiscale getTypeAutoriteFiscale();

	/**
	 * @return le numéro OFS de la collectivité (commune, pays)
	 */
	Integer getNumeroOfsAutoriteFiscale();
}
