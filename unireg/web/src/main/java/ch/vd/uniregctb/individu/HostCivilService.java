package ch.vd.uniregctb.individu;


public interface HostCivilService {

	/**
	 * Alimente une vue IndividuView en fonction du numero d'individu
	 *
	 * @return un objet IndividuView
	 */
	public IndividuView getIndividu(Long numeroIndividu);

	/**
	 * Retour l'utilisateur en fonction du numero d'individu
	 * @param numeroIndividu
	 * @return
	 */
	public String getNomUtilisateur(Long numeroIndividu) ;

}
