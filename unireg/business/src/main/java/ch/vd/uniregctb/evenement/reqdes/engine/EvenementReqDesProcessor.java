package ch.vd.uniregctb.evenement.reqdes.engine;

import java.util.Collection;

/**
 * Interface externe du processeur des événements ReqDes
 */
public interface EvenementReqDesProcessor {

	/**
	 * Interface à implémenter par celui qui veut être notifié de l'avancement des travaux
	 */
	public static interface Listener {

		/**
		 * Appelé à chaque fois qu'une unité de traitement a été traitée
		 * @param idUniteTraitement identifiant de l'unité traitée
		 */
		void onUniteTraitee(long idUniteTraitement);

		/**
		 * Appelé quand l'arrêt du processeur est demandé (= arrêt de l'application)
		 */
		void onStop();
	}

	/**
	 * Interface de marquage du handle servant à identifier un listener
	 */
	static interface ListenerHandle {}

	/**
	 * @param listener nouveau listener qui veut être notifié
	 * @return un handle qui devra être fourni à la méthode {@link #unregisterListener} à la fin de la période de notification
	 */
	ListenerHandle registerListener(Listener listener);

	/**
	 * @param handle handle retourné au moment de l'enregistrement du listener qui ne veut plus être notifié
	 */
	void unregisterListener(ListenerHandle handle);

	/**
	 * Demande le traitement asynchrone de l'unité de traitement identifiée par son ID technique
	 * @param id ID technique de l'unité de traitement à lancer
	 */
	void postUniteTraitement(long id);

	/**
	 * Demande le traitement asynchrone des unités de traitement identifiées par les IDs techniques présents dans la collection
	 * @param ids collection d'IDs techniques des unités de traitement à lancer
	 */
	void postUnitesTraitement(Collection<Long> ids);
}
