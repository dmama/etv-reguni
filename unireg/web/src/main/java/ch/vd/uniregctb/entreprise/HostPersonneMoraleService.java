package ch.vd.uniregctb.entreprise;

/**
 *  Re-organisation des informations de l'entreprise pour l'affichage Web
 *
 * @author xcifde
 *
 */
public interface HostPersonneMoraleService {

	/**
	 * Alimente une vue EntrepriseView en fonction du numero d'entreprise
	 *
	 * @return un objet EntrepriseView
	 */
	public EntrepriseView get(Long numeroEntreprise) ;

}
