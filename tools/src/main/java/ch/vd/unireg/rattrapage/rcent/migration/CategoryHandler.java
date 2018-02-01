package ch.vd.unireg.rattrapage.rcent.migration;

import java.text.ParseException;
import java.util.List;

/**
 * Interface des entités capables de regénérer les scripts SQL qui vont bien
 * depuis les logs extraits d'une certaine catégorie
 */
public interface CategoryHandler {

	/**
	 * Ajoute au buffer les scripts SQL qui vont bien
	 * @param buffer le {@link StringBuilder} où les scripts doivent être ajoutés
	 * @param input les données en entrée
	 * @throws ParseException en cas de ligne d'entrée un peu mal foutue...
	 */
	void buildSql(StringBuilder buffer, List<String> input) throws ParseException;
}
