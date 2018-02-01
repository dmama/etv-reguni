package ch.vd.unireg.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;

/**
 * Interface du processeur asynchrone des événements civils eCH
 */
public interface EvenementCivilEchProcessor {

	/**
	 * Interface à implémenter par celui qui veut être notifié de l'avancement des travaux
	 */
	interface Listener {

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
	 * Interface du handle servant à identifier un listener (et à le déconnecter)
	 */
	interface ListenerHandle {
		/**
		 * Méthode à appeler pour désactiver les notifications (un seul appel est permis)
		 * @throws IllegalStateException si plus d'un appel est fait sur cette méthode
		 */
		void unregister();
	}

	/**
	 * @param listener nouveau listener qui veut être notifié
	 * @return un handle qui devra être libéré à la fin de la période de notification (au travers de sa méthode {@link ListenerHandle#unregister() unregister})
	 */
	@NotNull
	ListenerHandle registerListener(Listener listener);

	/**
	 * Redémarre le thread de processing des événements civils (en douceur)
	 */
	void restartProcessingThread();
}
