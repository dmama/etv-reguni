package ch.vd.uniregctb.evenement.civil.engine;

import java.util.List;
import java.util.concurrent.TimeUnit;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.EtatEvenementCivil;
import ch.vd.uniregctb.type.TypeEvenementCivil;

/**
 * Interface du bean de gestion de la queue de notification de l'arrivée d'événements civils
 * à traiter (afin de gérer le traitement par file liée à un individu, le <i>token</i> de la
 * notification est le numéro d'individu)
 */
public interface EvenementCivilQueue {

	/**
	 * Informations de base sur un événement civil
	 */
	public static final class EvtCivilInfo {
		public final long idEvenement;
		public final EtatEvenementCivil etat;
		public final TypeEvenementCivil type;
		public final RegDate date;

		public EvtCivilInfo(long idEvenement, EtatEvenementCivil etat, TypeEvenementCivil type, RegDate date) {
			this.idEvenement = idEvenement;
			this.etat = etat;
			this.type = type;
			this.date = date;
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
