package ch.vd.unireg.interfaces.civil.data;

import ch.vd.registre.base.date.CollatableDateRange;

public interface RelationVersIndividu extends CollatableDateRange {

	/**
	 * @return Le numéro d'individu de l'autre partie
	 */
	long getNumeroAutreIndividu();

	/**
	 * @return le type de relation (= ce que représente l'autre partie pour cet individu)
	 */
	TypeRelationVersIndividu getTypeRelation();
}
