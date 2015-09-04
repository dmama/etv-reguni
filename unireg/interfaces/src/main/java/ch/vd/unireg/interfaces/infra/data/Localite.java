package ch.vd.unireg.interfaces.infra.data;

import ch.vd.registre.base.date.DateRange;

public interface Localite extends DateRange {

	Integer getChiffreComplementaire();

	String getNomAbrege();

	String getNom();

	Integer getNoOrdre();

	Integer getNPA();

	Integer getComplementNPA();

	Integer getNoCommune();

	/**
	 * @return une commune associée à la localité.
	 */
	Commune getCommuneLocalite();
}
