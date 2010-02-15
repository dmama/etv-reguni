package ch.vd.uniregctb.interfaces.service;

/**
 * Interface de notification de changements sur le service civil dont dépends Unireg.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public interface CivilListener {

	/**
	 * Cette méthode est appelée lorsque Unireg apprend qu'un individu a changé dans le registre civil.
	 * <p>
	 * Note: toute exception levée durant l'exécution du callback sera ignorée.
	 *
	 * @param numero
	 *            le numéro d'individu
	 */
	void onIndividuChange(long numero);
}
