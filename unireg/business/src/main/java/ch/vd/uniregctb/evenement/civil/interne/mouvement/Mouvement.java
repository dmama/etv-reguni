package ch.vd.uniregctb.evenement.civil.interne.mouvement;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Adresse;


public interface Mouvement extends EvenementCivilInterne {
	/**
	 * Renvoie l'adresse principale de l'individu après le départ ou l'arrivée.
	 */
	Adresse getNouvelleAdressePrincipale();


}
