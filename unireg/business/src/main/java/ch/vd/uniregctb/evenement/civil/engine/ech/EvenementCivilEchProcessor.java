package ch.vd.uniregctb.evenement.civil.engine.ech;

/**
 * Interface du processeur asynchrone des événements civils eCH
 */
public interface EvenementCivilEchProcessor {

	/**
	 * Interface à implémenter par celui qui veut être notifié de l'avancement des travaux
	 */
	public static interface Listener {

		/**
		 * Appelé à chaque fois qu'un lot d'événements civils est terminé
		 * @param noIndividu identifiant de l'individu pour lequel le lot est terminé
		 */
		void onIndividuTraite(long noIndividu);

		/**
		 * Appelé quand l'arrêt du processeur est demandé (= arrêt de l'application)
		 */
		void onStop();
	}

	/**
	 * Interface de marquage du handle servant à identifier un listener
	 */
	public static interface ListenerHandle {}

	/**
	 * @param listener nouveau listener qui veut être notifié
	 * @return un handle qui devra être fourni à la méthode {@link #unregisterListener} à la fin de la période de notification
	 */
	ListenerHandle registerListener(Listener listener);

	/**
	 * @param handle handle retourné au moment de l'enregistrement du listener qui ne veut plus être notifié
	 */
	void unregisterListener(ListenerHandle handle);
}
