package ch.vd.uniregctb.evenement;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.type.TypeEvenementCivil;

public interface EvenementCivil {

	/**
	 * Renvoie l'adresse principale de l'individu concerné à la veille de
	 * l'événement.
	 *
	 * @return the adresse
	 */
	Adresse getAdressePrincipale();

	/**
	 * Renvoie l'adresse secondaire (= l'adresse de la résidence secondaire dans
	 * le canton dans le cas d'un individu possédant sa résidence principale
	 * hors canton)
	 *
	 * @return l'adresse si l'individu possède une résidence secondaire, null autrement.
	 */
	Adresse getAdresseSecondaire();

	/**
	 * Renvoie l'adresse courrier de l'individu concerné à la veille de
	 * l'événement.
	 *
	 * @return the adresseCourrier
	 */
	Adresse getAdresseCourrier();

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
