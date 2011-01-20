package ch.vd.uniregctb.evenement.obtentionpermis;

import ch.vd.uniregctb.evenement.EvenementCivil;

/**
 * Evenement d'obtention d'une nationalité.
 */
public interface ObtentionNationalite extends EvenementCivil{
	
	/**
	 * Renvoie le numéro OFS étendu de la commune de l'adresse principale (gestion des fractions)
	 */
	Integer getNumeroOfsEtenduCommunePrincipale();
}
