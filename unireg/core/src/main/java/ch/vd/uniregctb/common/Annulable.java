package ch.vd.uniregctb.common;

/**
 * Interface permettant de manipuler toutes les entités pouvant être annulé
 * @author Baba NGOM <baba-issa.ngom@vd.ch>
 */
public interface Annulable {

	/**
	 * @return <b>true</b> si l'entite est annulée; <b>false</b> si elle ne l'est pas.
	 */
	boolean isAnnule();

	/**
	 * @return <b>false</b> si l'entite n'est pas annulée; <b>oui</b> si elle l'est.
	 */
	default boolean isNotAnnule() {
		return !this.isAnnule();
	}
}
