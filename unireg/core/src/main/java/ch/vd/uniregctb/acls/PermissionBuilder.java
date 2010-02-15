package ch.vd.uniregctb.acls;

import org.acegisecurity.acls.Permission;

/**
 * Interface permettant de créer un objet Permission 
 * à partir d'un masque binaire.
 * 
 * @author Ludovic Bertin (OOsphere)
 *
 */
public interface PermissionBuilder {
	
	/**
     * Crée dynamiquement un <code>Permission</code> 
     * représentant les bits actifs dans le masque donnée en paramètre.
     * 
	 * @param mask	le masque binaire
	 * 
	 * @return une permission correspondant au masque
	 */
	Permission buildFromMask(int mask);
}
