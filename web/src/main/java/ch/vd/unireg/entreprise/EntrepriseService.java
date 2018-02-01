package ch.vd.unireg.entreprise;

import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;

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
	EntrepriseView getEntreprise(Entreprise entreprise) ;

	/**
	 * Alimente une vue EtablissementView en fonction du numero d'etablissement
	 *
	 * @return un objet EtablissementView
	 */
	EtablissementView getEtablissement(Etablissement etablissement);
}
