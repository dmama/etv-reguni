package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
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
	public static final class Batch {

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
			this.contenu = contenu != null ? contenu : Collections.<EvenementCivilEchBasicInfo>emptyList();
		}
	}

	/**
	 * Méthode utilisée pour ajouter des éléments à la queue
	 * @param noIndividu numéro de l'individu qui vient de recevoir un événement
	 * @param immediate détermine si oui ou non le décalage temporel doit être appliqué
	 * @throws NullPointerException en cas de paramètre <code>null</code>
	 */
	void post(Long noIndividu, boolean immediate);

	/**
	 * Méthode utilisée pour ajouter des éléments à la queue en bloc (le décalage sera forcément appliqué)
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
	int getInflightCount();
}
