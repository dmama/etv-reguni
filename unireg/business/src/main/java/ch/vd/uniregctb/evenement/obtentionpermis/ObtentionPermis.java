package ch.vd.uniregctb.evenement.obtentionpermis;

import ch.vd.uniregctb.evenement.EvenementCivil;
import ch.vd.registre.civil.model.EnumTypePermis;

/**
 * Evenement d'obtention d'un permis.
 */
public interface ObtentionPermis extends EvenementCivil{
	
	/**
	 * Type du permis obtenu.
	 */
	EnumTypePermis getTypePermis();
	
	/**
	 * Renvoie le numéro OFS étendu de la commune de l'adresse principale (gestion des fractions)
	 */
	Integer getNumeroOfsEtenduCommunePrincipale();
}
