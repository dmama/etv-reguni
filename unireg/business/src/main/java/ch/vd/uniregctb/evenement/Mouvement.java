package ch.vd.uniregctb.evenement;

import ch.vd.uniregctb.interfaces.model.Adresse;


public interface Mouvement extends EvenementCivil {
	/**
	 * Renvoie l'adresse principale de l'individu après le départ ou l'arrivée.
	 */
	Adresse getNouvelleAdressePrincipale();


}
