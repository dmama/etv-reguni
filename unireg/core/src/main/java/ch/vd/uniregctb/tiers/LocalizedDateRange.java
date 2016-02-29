package ch.vd.uniregctb.tiers;

import ch.vd.registre.base.date.DateRange;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Interface qui définit un accès à une localisation (= type autorité fiscale + numéro OFS) sur une plage de validité ({@link DateRange})
 */
public interface LocalizedDateRange extends DateRange {

	/**
	 * @return le type d'autorité fiscale
	 */
	TypeAutoriteFiscale getTypeAutoriteFiscale();

	/**
	 * @return le numéro OFS de la collectivité (commune, pays)
	 */
	Integer getNumeroOfsAutoriteFiscale();
}
