package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchProcessingMode;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Interface du bean de gestion de la queue de notification de l'arrivée d'événements civils
 * à traiter (afin de gérer le traitement par file liée à un individu, le <i>token</i> de la
 * notification est le numéro d'individu)
 */
public interface EvenementCivilNotificationQueue {

	/**
	 * Lot d'événements civils à traiter pour un individu donné
	 */
	final class Batch {

		/**
		 * Identifiant de l'individu pour lequel le lot est constitué
		 */
		public final long noIndividu;

		/**
		 * Informations sur les événements civils à traiter.
		 * L'ordre de tri des éléments de cette liste est calculé ainsi :
		 * <ol>
		 *     <li>la date de l'événement (élément {@link EvenementCivilEchBasicInfo#date}), du plus ancien au plus récent</li>
		 *     <li>à dates égales, selon la {@link TypeEvenementCivilEch#priorite priorité} associée au type (élément {@link EvenementCivilEchBasicInfo#type}) de l'événement, de la plus petite à la plus grande</li>
		 * </ol>
		 * <p/>
		 * <b>Nota bene :</b> cette liste peut être vide, mais jamais nulle
		 */
		public final List<EvenementCivilEchBasicInfo> contenu;

		protected Batch(long noIndividu, @Nullable List<EvenementCivilEchBasicInfo> contenu) {
			this.noIndividu = noIndividu;
			this.contenu = contenu != null ? contenu : Collections.emptyList();
		}
	}

	/**
	 * Méthode utilisée pour ajouter des éléments à la queue de traitment.
	 * @param noIndividu numéro de l'individu qui vient de recevoir un événement
	 * @param mode mode de traitement
	 * @throws NullPointerException en cas de paramètre <code>null</code>
	 */
	void post(Long noIndividu, EvenementCivilEchProcessingMode mode);

	/**
	 * Méthode utilisée pour ajouter des éléments à la queue des traitements batch en bloc (le décalage sera forcément appliqué)
	 * @param nosIndividus collections de numéros d'individus à poster
	 */
	void postAll(Collection<Long> nosIndividus);

	/**
	 * Va chercher le prochain lot de traitement d'événements pour un individu
	 * @param timeout temps d'attente maximum avant de rendre la main
	 * @param unit unité du temps d'attente maximum
	 * @return si aucun événement, <code>null</code> ; sinon, la liste triée (premier dans la liste = plus ancien) des événements à traiter sur le prochain individu. <b>Nota bene:</b> la liste en question peut être vide...
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
	 * @return le nombre d'éléments actuellement en attente de traitement dans la queue interne "manual"
	 */
	int getInManualQueueCount();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "manual" à la queue finale sur les 5 dernières minutes
	 */
	Long getManualQueueSlidingAverageAge();

	/**
	 * @return l'âge moyen (en millisecondes) d'un élément lorsqu'il passe de la queue interne "manual" à la queue finale depuis le démarrage du service
	 */
	Long getManualQueueGlobalAverageAge();

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
