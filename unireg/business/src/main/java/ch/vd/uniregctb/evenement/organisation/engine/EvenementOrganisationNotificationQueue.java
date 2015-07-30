package ch.vd.uniregctb.evenement.organisation.engine;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationProcessingMode;

/**
 * Interface du bean de gestion de la queue de notification de l'arrivée d'événements organisation
 * à traiter (afin de gérer le traitement par file liée à une organisation, le <i>token</i> de la
 * notification est le numéro d'organisation)
 */
public interface EvenementOrganisationNotificationQueue {

	/**
	 * Lot d'événements organisation à traiter pour une organisation donnée
	 */
	final class Batch {

		/**
		 * Identifiant de l'organisation pour lequel le lot est constitué
		 */
		public final long noOrganisation;

		/**
		 * Informations sur les événements organisation à traiter.
		 * L'ordre de tri des éléments de cette liste est calculé ainsi :
		 * <ol>
		 *     <li>la date de l'événement (élément {@link EvenementOrganisationBasicInfo#date}), du plus ancien au plus récent</li>
		 *     <li>à dates égales, dans l'ordre croissant des identifiants</li>
		 * </ol>
		 * <p/>
		 * <b>Nota bene :</b> cette liste peut être vide, mais jamais nulle
		 */
		public final List<EvenementOrganisationBasicInfo> contenu;

		protected Batch(long noOrganisation, @Nullable List<EvenementOrganisationBasicInfo> contenu) {
			this.noOrganisation = noOrganisation;
			this.contenu = contenu != null ? contenu : Collections.<EvenementOrganisationBasicInfo>emptyList();
		}
	}

	/**
	 * Méthode utilisée pour ajouter des éléments à la queue de traitment.
	 * @param noOrganisation numéro de l'organisation qui vient de recevoir un événement
	 * @param mode mode de traitement
	 * @throws NullPointerException en cas de paramètre <code>null</code>
	 */
	void post(Long noOrganisation, EvenementOrganisationProcessingMode mode);

	/**
	 * Méthode utilisée pour ajouter des éléments à la queue des traitements batch en bloc (le décalage sera forcément appliqué)
	 * @param nosOrganisation collections de numéros d'organisation à poster
	 *
	 */
	void postAll(Collection<Long> nosOrganisation);

	/**
	 * Va chercher le prochain lot de traitement d'événements pour un organisation
	 * @param timeout temps d'attente maximum avant de rendre la main
	 * @param unit unité du temps d'attente maximum
	 * @return si aucun événement, <code>null</code> ; sinon, la liste triée (premier dans la liste = plus ancien) des événements à traiter sur la prochain organisation. <b>Nota bene:</b> la liste en question peut être vide...
	 * @throws InterruptedException en cas d'interruption du thread pendant l'attente
	 */
	Batch poll(long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * @return le nombre d'éléments actuellement en attente de traitement dans la queue
	 */
	int getTotalCount();

	/**
	 * @return le nombre d'éléments actuellement en attente de traitement dans la queue interne "batch"
	 */
	int getInBatchQueueCount();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "batch" à la queue finale sur les 5 dernières minutes
	 */
	Long getBatchQueueSlidingAverageAge();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "batch" à la queue finale depuis le démarrage du service
	 */
	Long getBatchQueueGlobalAverageAge();

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
	 * la queue manual ou batch et la queue finale
	 */
	int getInHatchesCount();
}
