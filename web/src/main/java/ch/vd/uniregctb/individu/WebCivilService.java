package ch.vd.uniregctb.individu;


public interface WebCivilService {

	/**
	 * Alimente une vue IndividuView en fonction du numero d'individu
	 *
	 * @param numeroIndividu le numéro de l'individu en question
	 * @return un objet IndividuView representat l'individu
	 */
	IndividuView getIndividu(Long numeroIndividu);

	/**
	 * Alimente une vue IndividuView en fonction du numero d'individu.
	 * Si on ne trouve pas l'individu grace à son numéro on essaye de retrouvé les info grace a un numéro d'evenement
	 *
	 * @param numeroIndividu le numéro de le numéro de l'individu en question
	 * @param numeroEvenement Le numero de l'evenement de backup
	 * @return un objet IndividuView
	 */
	IndividuView getIndividu(Long numeroIndividu, Long numeroEvenement);
}
