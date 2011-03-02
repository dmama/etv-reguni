package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Evenement d'obtention d'un permis.
 */
public interface ObtentionPermis extends EvenementCivilInterne {
	
	/**
	 * Type du permis obtenu.
	 */
	TypePermis getTypePermis();
	
	/**
	 * Renvoie le numéro OFS étendu de la commune de l'adresse principale (gestion des fractions)
	 */
	Integer getNumeroOfsEtenduCommunePrincipale();
}
