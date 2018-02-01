package ch.vd.unireg.evenement.reqdes.engine;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

/**
 * Interface externe du processeur des événements ReqDes
 */
public interface EvenementReqDesProcessor {

	/**
	 * Interface à implémenter par celui qui veut être notifié de l'avancement des travaux
	 */
	interface Listener {

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
	 * @return un handle qui devra être dés-inscrit à la fin de la période de notification (au travers de sa méthode {@link ListenerHandle#unregister() unregister})
	 */
	@NotNull
	ListenerHandle registerListener(Listener listener);

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
