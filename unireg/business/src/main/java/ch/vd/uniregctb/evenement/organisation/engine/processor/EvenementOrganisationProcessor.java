package ch.vd.uniregctb.evenement.organisation.engine.processor;

/**
 * Interface du processeur asynchrone des événements organisation
 */
public interface EvenementOrganisationProcessor {

	/**
	 * Interface à implémenter par celui qui veut être notifié de l'avancement des travaux
	 */
	interface Listener {

		/**
		 * Appelé à chaque fois qu'un lot d'événements organisation est terminé
		 * @param noOrganisation identifiant de l'individu pour lequel le lot est terminé
		 */
		void onOrganisationTraite(long noOrganisation);

		/**
		 * Appelé quand l'arrêt du processeur est demandé (= arrêt de l'application)
		 */
		void onStop();
	}

	/**
	 * Interface de marquage du handle servant à identifier un listener
	 */
	interface ListenerHandle {}

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
	 * Redémarre le thread de processing des événements organisation
	 * @param aggressiveKill si <code>true</code>, force l'interruption du thread, sinon un stop en douceur est demandé
	 */
	void restartProcessingThread(boolean aggressiveKill);
}
