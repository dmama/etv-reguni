package ch.vd.uniregctb.etiquette;

import ch.vd.registre.base.date.RegDate;

@FunctionalInterface
public interface DecalageDate {

	/**
	 * Décale la date du nombre d'unités (spécifique à l'implémentation) donné
	 * @param date date de départ
	 * @param decalage valeur du décalage
	 * @return date décalée
	 */
	RegDate apply(RegDate date, int decalage);
}
