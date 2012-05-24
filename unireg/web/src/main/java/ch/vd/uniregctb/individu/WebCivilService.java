package ch.vd.uniregctb.individu;


public interface WebCivilService {

	/**
	 * Alimente une vue IndividuView en fonction du numero d'individu
	 *
	 * @return un objet IndividuView
	 */
	public IndividuView getIndividu(Long numeroIndividu);

}
