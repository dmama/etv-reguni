package ch.vd.uniregctb.evenement;

import ch.vd.uniregctb.interfaces.model.Adresse;

public interface EvenementCivilAvecAdresses extends EvenementCivil {

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

}
