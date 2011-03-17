package ch.vd.uniregctb.evenement.obtentionpermis;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.uniregctb.type.TypePermis;

/**
 * Evenement d'obtention d'un permis.
 */
public interface ObtentionPermis extends EvenementCivil{
	
	/**
	 * Type du permis obtenu.
	 */
	TypePermis getTypePermis();
	
	/**
	 * Renvoie le numéro OFS étendu de la commune de l'adresse principale (gestion des fractions)
	 */
	Integer getNumeroOfsEtenduCommunePrincipale();
}
