package ch.vd.uniregctb.evenement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public interface EvenementCivil {

	/**
	 * @return the type
	 */
	TypeEvenementCivil getType();

	/**
	 * @return the conjoint
	 */
	Individu getConjoint();

	/**
	 * @return the date
	 */
	RegDate getDate();

	/**
	 * @return the individu
	 */
	Individu getIndividu();

	/**
	 * @return the numeroEvenement
	 */
	Long getNumeroEvenement();

	/**
	 * @return the numeroOfsCommuneAnnonce
	 */
	Integer getNumeroOfsCommuneAnnonce();

	/**
	 * Renvoie vrai (par défaut) si le contribuable est présent avant le
	 * traitement Ceci est faux pour le cas d'une naissance par exemple
	 *
	 * @return boolean
	 */
	boolean isContribuablePresentBefore();

}
