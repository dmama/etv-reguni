package ch.vd.uniregctb.entreprise;

import ch.vd.uniregctb.tiers.Entreprise;

/**
 *  Re-organisation des informations de l'entreprise pour l'affichage Web
 *
 * @author xcifde
 *
 */
public interface EntrepriseService {

	/**
	 * Alimente une vue EntrepriseView en fonction du numero d'entreprise
	 *
	 * @return un objet EntrepriseView
	 */
	EntrepriseView get(Entreprise entreprise) ;
}
