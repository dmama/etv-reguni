package ch.vd.uniregctb.evenement.civil.interne.obtentionpermis;

import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;

/**
 * Evenement d'obtention d'une nationalité.
 */
public interface ObtentionNationalite extends EvenementCivilInterne {
	
	/**
	 * Renvoie le numéro OFS étendu de la commune de l'adresse principale (gestion des fractions)
	 */
	Integer getNumeroOfsEtenduCommunePrincipale();
}
