package ch.vd.uniregctb.common;

/**
 * Interface permettant de manipuler toutes les entités pouvant être annulé
 * @author Baba NGOM <baba-issa.ngom@vd.ch>
 */
public interface Annulable {

	/**Permet de dire si une entite est annulé ou non
	 *
	 * @return true si l'entite est annulée false sinon
	 */
	public boolean isAnnule();

}
