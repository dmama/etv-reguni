package ch.vd.unireg.individu;


public interface WebCivilService {

	/**
	 * Alimente une vue IndividuView en fonction du numero d'individu
	 *
	 * @param numeroIndividu le numéro de l'individu en question
	 * @return un objet IndividuView representat l'individu
	 * @throws ch.vd.unireg.common.ObjectNotFoundException si on ne retrouve pas d'individu correspondant
	 */
	IndividuView getIndividu(Long numeroIndividu);

	/**
	 * Alimente une vue IndividuView en fonction du numero d'individu. Si on ne trouve pas l'individu grace à son numéro on essaye de retrouvé les info grace a un numéro d'evenement
	 *
	 * @param numeroIndividu  le numéro de le numéro de l'individu en question
	 * @param numeroEvenement Le numero de l'evenement de backup
	 * @return un objet IndividuView
	 * @throws ch.vd.unireg.common.ObjectNotFoundException Si on ne trouve l'individu ni pas son numero ou par un numero d'evt
	 */
	IndividuView getIndividu(Long numeroIndividu, Long numeroEvenement);
}
