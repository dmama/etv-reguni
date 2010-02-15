package ch.vd.uniregctb.evenement.demenagement;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.Commune;

/**
 * Modèlise le déménagement intra communal d'un individu
 *
 * @author Ludovic Bertin
 *
 */
public interface Demenagement extends EvenementCivil {

	/**
	 * Renvoie l'adresse principale de l'individu après le déménagement.
	 */
	Adresse getNouvelleAdressePrincipale();

	/**
	 * Renvoie la commune de la nouvelle adresse principale de l'individu après le départ.
	 */
	Commune getNouvelleCommunePrincipale();
	
	/**
	 * Renvoie l'ancienne adresse principale de l'individu avant le déaprt
	 */
	Adresse getAncienneAdressePrincipale();
}
