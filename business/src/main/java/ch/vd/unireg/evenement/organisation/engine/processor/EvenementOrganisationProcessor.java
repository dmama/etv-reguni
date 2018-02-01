package ch.vd.uniregctb.evenement.organisation.engine.processor;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;

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
	 * Redémarre le thread de processing des événements organisation (en douceur)
	 */
	void restartProcessingThread();

	/**
	 * Force un évenement
	 *
	 * @param evt Descripteur de l'événement à forcer
	 */
	void forceEvenement(EvenementOrganisationBasicInfo evt);

}
