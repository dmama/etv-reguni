package ch.vd.unireg.common;

/**
 * Interface permettant de manipuler toutes les entités pouvant être annulées
 * @author Baba NGOM <baba-issa.ngom@vd.ch>
 * @see AnnulableHelper pour quelques méthodes utilitaires de manipulation des entités annulables
 */
public interface Annulable {

	/**
	 * @return <b>true</b> si l'entite est annulée; <b>false</b> si elle ne l'est pas.
	 */
	boolean isAnnule();

}
