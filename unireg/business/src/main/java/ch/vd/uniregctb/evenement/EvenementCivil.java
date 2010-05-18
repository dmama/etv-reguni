package ch.vd.uniregctb.evenement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public interface EvenementCivil {

	/**
	 * @return the type
	 */
	TypeEvenementCivil getType();

	/**
	 * @return the date
	 */
	RegDate getDate();

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

	/**
	 * @return le numéro de l'individu principal
	 */
	Long getNoIndividu();

	/**
	 * @return the individu
	 */
	Individu getIndividu();

	/**
	 * @return l'id de la personne physique correspondant à l'individu; ou <b>null</b> si aucune personne physique ne correspond à l'individu.
	 */
	Long getPrincipalPPId();

	/**
	 * @return le numéro d'individu du conjoint
	 */
	Long getNoIndividuConjoint();
	
	/**
	 * @return the conjoint
	 */
	Individu getConjoint();

	/**
	 * @return l'id de la personne physique correspondant au conjoint de l'individu; ou <b>null</b> si aucune personne physique ne correspond.
	 */
	Long getConjointPPId();
}
