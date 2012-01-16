package ch.vd.uniregctb.evenement.civil.engine;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.ActionEvenementCivilEch;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivilEch;

/**
 * Interface du bean de gestion de la queue de notification de l'arrivée d'événements civils
 * à traiter (afin de gérer le traitement par file liée à un individu, le <i>token</i> de la
 * notification est le numéro d'individu)
 */
public interface EvenementCivilNotificationQueue {

	/**
	 * Informations de base sur un événement civil.
	 * <p/>
	 * L'ordre de tri naturel des éléments de cette classe est calculé ainsi :
	 * <ol>
	 *     <li>la date de l'événement (élément {@link #date}), du plus ancien au plus récent</li>
	 *     <li>à dates égales, selon la {@link TypeEvenementCivilEch#priorite priorité} associée au type (élément {@link #type}) de l'événement, de la plus petite à la plus grande</li>
	 * </ol>
	 */
	public static final class EvtCivilInfo {
		public final long idEvenement;
		public final long noIndividu;
		public final EtatEvenementCivil etat;
		public final TypeEvenementCivilEch type;
		public final ActionEvenementCivilEch action;
		public final Long idEvenementReference;
		public final RegDate date;

		public EvtCivilInfo(long idEvenement, long noIndividu, EtatEvenementCivil etat, TypeEvenementCivilEch type, ActionEvenementCivilEch action, @Nullable Long idEvenementReference, RegDate date) {
			this.idEvenement = idEvenement;
			this.noIndividu = noIndividu;
			this.etat = etat;
			this.type = type;
			this.action = action;
			this.idEvenementReference = idEvenementReference;
			this.date = date;

			if (this.date == null) {
				throw new IllegalArgumentException("La date de l'événement ne doit pas être nulle");
			}
			if (this.type == null) {
				throw new NullPointerException("Le type de l'événement ne doit pas être nul");
			}
		}
	}

	/**
	 * Méthode utilisée pour ajouter des éléments à la queue
	 * @param noIndividu numéro de l'individu qui vient de recevoir un événement
	 * @throws NullPointerException en cas de paramètre <code>null</code>
	 */
	void add(Long noIndividu);

	/**
	 * Va chercher le prochain lot de traitement d'événements pour un individu
	 * @param timeout temps d'attente maximum avant de rendre la main
	 * @param unit unité du temps d'attente maximum
	 * @return si aucun événement, <code>null</code> ; sinon, la liste triée (premier dans la liste = plus ancien) des événements à traiter sur le prochain individu. <b>Nota bene:</b> la liste en question peut être vide...
	 * @throws InterruptedException en cas d'interruption du thread pendant l'attente
	 */
	List<EvtCivilInfo> poll(long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * @return le nombre d'éléments actuellement en attente de traitement dans la queue
	 */
	int getInflightCount();
}
