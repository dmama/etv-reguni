package ch.vd.unireg.evenement.organisation.engine;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.evenement.organisation.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseProcessingMode;

/**
 * Interface du bean de gestion de la queue de notification de l'arrivée d'événements entreprise
 * à traiter (afin de gérer le traitement par file liée à une entreprise, le <i>token</i> de la
 * notification est le numéro d'entreprise)
 */
public interface EvenementEntrepriseNotificationQueue {

	/**
	 * Lot d'événements entreprise à traiter pour une entreprise donnée
	 */
	final class Batch {

		/**
		 * Identifiant de l'entreprise pour lequel le lot est constitué
		 */
		public final long noEntrepriseCivile;

		/**
		 * Informations sur les événements entreprise à traiter.
		 * L'ordre de tri des éléments de cette liste est calculé ainsi :
		 * <ol>
		 *     <li>la date de l'événement (élément {@link EvenementEntrepriseBasicInfo#date}), du plus ancien au plus récent</li>
		 *     <li>à dates égales, dans l'ordre croissant des identifiants</li>
		 * </ol>
		 * <p/>
		 * <b>Nota bene :</b> cette liste peut être vide, mais jamais nulle
		 */
		public final List<EvenementEntrepriseBasicInfo> contenu;

		protected Batch(long noEntrepriseCivile, @Nullable List<EvenementEntrepriseBasicInfo> contenu) {
			this.noEntrepriseCivile = noEntrepriseCivile;
			this.contenu = contenu != null ? contenu : Collections.emptyList();
		}
	}

	/**
	 * Méthode utilisée pour ajouter des éléments à la queue de traitment.
	 * @param noEntrepriseCivile numéro de l'entreprise qui vient de recevoir un événement
	 * @param mode mode de traitement
	 * @throws NullPointerException en cas de paramètre <code>null</code>
	 */
	void post(Long noEntrepriseCivile, EvenementEntrepriseProcessingMode mode);

	/**
	 * Méthode utilisée pour ajouter des éléments à la queue des traitements bulk en bloc (le décalage sera forcément appliqué)
	 * @param nosEntreprisesCiviles collections de numéros d'entreprise à poster
	 *
	 */
	void postAll(Collection<Long> nosEntreprisesCiviles);

	/**
	 * Va chercher le prochain lot de traitement d'événements pour une entreprise
	 * @param timeout temps d'attente maximum avant de rendre la main
	 * @return si aucun événement, <code>null</code> ; sinon, la liste triée (premier dans la liste = plus ancien) des événements à traiter sur la prochaine entreprise. <b>Nota bene:</b> la liste en question peut être vide...
	 * @throws InterruptedException en cas d'interruption du thread pendant l'attente
	 */
	Batch poll(Duration timeout) throws InterruptedException;

	/**
	 * @return le nombre d'éléments actuellement en attente de traitement dans la queue
	 */
	int getTotalCount();

	/**
	 * @return le nombre d'éléments actuellement en attente de traitement dans la queue interne "bulk"
	 */
	int getInBulkQueueCount();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "bulk" à la queue finale sur les 5 dernières minutes
	 */
	Long getBulkQueueSlidingAverageAge();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "bulk" à la queue finale depuis le démarrage du service
	 */
	Long getBulkQueueGlobalAverageAge();

	/**
	 * @return le nombre d'éléments actuellement en attente de traitement dans la queue interne "priority"
	 */
	int getInPriorityQueueCount();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "priority" à la queue finale sur les 5 dernières minutes
	 */
	Long getPriorityQueueSlidingAverageAge();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "priority" à la queue finale depuis le démarrage du service
	 */
	Long getPriorityQueueGlobalAverageAge();

	/**
	 * @return le nombre d'éléments actuellement en attente de traitement dans la queue interne "immediate"
	 */
	int getInImmediateQueueCount();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "immediate" à la queue finale sur les 5 dernières minutes
	 */
	Long getImmediateQueueSlidingAverageAge();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "immediate" à la queue finale depuis le démarrage du service
	 */
	Long getImmediateQueueGlobalAverageAge();

	/**
	 * @return le nombre d'éléments actuellement en attente de traitement dans la queue interne "final"
	 */
	int getInFinalQueueCount();

	/**
	 * @return le nombre d'éléments actuellement en attente de traitement en transition entre
	 * la queue manual ou bulk et la queue finale
	 */
	int getInHatchesCount();
}
