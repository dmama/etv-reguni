package ch.vd.unireg.evenement.entreprise.engine.processor;

import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;

/**
 * Interface du processeur asynchrone des événements entreprise
 */
public interface EvenementEntrepriseProcessor {

	/**
	 * Interface à implémenter par celui qui veut être notifié de l'avancement des travaux
	 */
	interface Listener {

		/**
		 * Appelé à chaque fois qu'un lot d'événements entreprise est terminé
		 * @param noEntrepriseCivile numéro cantonal de l'entreprise pour lequel le lot est terminé
		 */
		void onEntrepriseTraitee(long noEntrepriseCivile);

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
	 * @return un handle qui doit servir à désactiver les notifications une fois les traitements terminés
	 */
	@NotNull
	ListenerHandle registerListener(Listener listener);

	/**
	 * Redémarre le thread de processing des événements entreprise (en douceur)
	 */
	void restartProcessingThread();

	/**
	 * Force un évenement
	 *
	 * @param evt Descripteur de l'événement à forcer
	 */
	void forceEvenement(EvenementEntrepriseBasicInfo evt);

}
