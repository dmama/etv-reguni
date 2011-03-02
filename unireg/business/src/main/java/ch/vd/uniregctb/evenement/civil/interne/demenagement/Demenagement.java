package ch.vd.uniregctb.evenement.civil.interne.demenagement;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.interfaces.model.Adresse;
import ch.vd.uniregctb.interfaces.model.CommuneSimple;

/**
 * Modèlise le déménagement intra communal d'un individu
 *
 * @author Ludovic Bertin
 *
 */
public interface Demenagement extends EvenementCivilInterne {

	/**
	 * Renvoie l'adresse principale de l'individu après le déménagement.
	 */
	Adresse getNouvelleAdressePrincipale();

	/**
	 * Renvoie la commune de la nouvelle adresse principale de l'individu après le départ.
	 */
	CommuneSimple getNouvelleCommunePrincipale();
	
	/**
	 * Renvoie l'ancienne adresse principale de l'individu avant le déaprt
	 */
	Adresse getAncienneAdressePrincipale();
}
