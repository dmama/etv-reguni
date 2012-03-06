package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collection;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchService;

/**
 * Classe utilitaire qui joue le rôle d'une queue bloquante pour le traitement des événements civils reçus de RCPers
 * <ul>
 *     <li>En entrée, les éléments postés dans cette queue sont des numéros d'individu</li>
 *     <li>En sortie, les éléments sont des listes d'événements civils (en fait, structures {@link EvenementCivilEchBasicInfo})</li>
 * </ul>
 * <p/>
 * <b>Quels sont les besoins de {@link java.util.concurrent.locks.Lock Lock} supplémentaire ?</b>
 * <p/>
 * Et bien voilà... je m'étais dit au départ que la méthode {@link #post} devrait faire en sorte d'éliminer les doublons de numéros d'individu...
 * Dans ce cadre, afin de bien les filtrer, il était important de faire en sorte que deux appels à {@link BlockingQueue#add} ne devaient
 * pas être faits en même temps, d'où l'utilisation d'un {@link java.util.concurrent.locks.ReentrantLock ReentrantLock}.
 * <p/>
 * Mais du coup, il était assez raisonnable de penser que la méthode {@link BlockingQueue#poll} devait subir le même genre de contrainte. Et si
 * c'est bien le cas, alors cela signifie qu'aucun appel à {@link #post} ne pourrait être fait pendant que la méthode {@link #poll(long, java.util.concurrent.TimeUnit) poll}
 * est en attente sur l'appel à {@link BlockingQueue#poll}, ce qui n'est pas très bon en terme de performances...
 * <p/>
 * Il restait donc deux axes :
 * <ul>
 *     <li>ne poser un verrou que sur la partie {@link BlockingQueue#add}, ou</li>
 *     <li>ne pas poser de verrou du tout.</li>
 * </ul>
 * <p/>
 * Dans le premier cas, nous conservons une tentative d'élimination de doublons efficace (ce que ne nous offre pas le second cas), et
 * le pire qui peut se produire est que l'élément soit enlevé de la queue entre le moment où on a déterminé qu'il y était déjà (et donc
 * le moment où on a décidé de ne pas l'y rajouter à nouveau) et le moment où on relâche le verrou. En effet, il ne peut pas y avoir,
 * par construction, d'autres ajouts dans cette queue au même moment.
 * <p/>
 * Si on suppose donc (ce qui devrait être le cas, de la manière dont
 * je vois l'implémentation de l'appelant de la méthode {@link #post}) que <i>le nouvel événement est déjà committé en base</i> au moment
 * où l'appel à la méthode {@link #post} est lancé, alors le thread qui est justement en train de récupérer les événements civils de cet
 * individu va de toute façon également récupérer cet événement-là.
 * <p/>
 * En revanche, si on choisit le second cas, alors on perd l'élimination des doublons, et on risque relativement souvent de voir la méthode
 * {@link #poll(long, java.util.concurrent.TimeUnit) poll} faire une requête en base dans le vide (car les événements auraient déjà été traités par
 * le passage précédent de la valeur en doublon). Notons bien que ce cas n'est pas totalement exclu dans la première solution, dans le
 * cas où l'identifiant de l'individu est enlevé de la queue entre le moment où l'événenement correspondant est effectivement committé en base
 * et le moment où la méthode {@link #post(Long) post} vérifie sa présence... mais cela devrait se produire moins souvent.
 */
public class EvenementCivilNotificationQueueImpl implements EvenementCivilNotificationQueue {

	private final BlockingQueue<Long> queue = new LinkedBlockingQueue<Long>();
	private final ReentrantLock lock = new ReentrantLock();

	private EvenementCivilEchService evtCivilService;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtCivilService(EvenementCivilEchService evtCivilService) {
		this.evtCivilService = evtCivilService;
	}

	@Override
	public void post(Long noIndividu) {
		lock.lock();
		try {
			internalPost(noIndividu);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void postAll(Collection<Long> nosIndividus) {
		if (nosIndividus != null && nosIndividus.size() > 0) {
			lock.lock();
			try {
				for (Long noIndividu : nosIndividus) {
					internalPost(noIndividu);
				}
			}
			finally {
				lock.unlock();
			}
		}
	}

	/**
	 * Doit impérativement être appelé avec le verrou {@link #lock} possédé
	 * @param noIndividu numéro d'individu à poster dans la queue interne
	 */
	private void internalPost(Long noIndividu) {
		Assert.isTrue(lock.isHeldByCurrentThread());

		if (noIndividu == null) {
			throw new NullPointerException("noIndividu");
		}

		if (!queue.contains(noIndividu)) {
			queue.add(noIndividu);
		}
	}
	
	@Override
	public Batch poll(long timeout, TimeUnit unit) throws InterruptedException {
		final Long noIndividu = queue.poll(timeout, unit);
		if (noIndividu != null) {
			// 1. trouve tous les événements civils de cet individu qui sont dans un état A_TRAITER, EN_ATTENTE, EN_ERREUR
			// 2. tri de ces événements par date, puis type d'événement
			return new Batch(noIndividu, evtCivilService.buildLotEvenementsCivils(noIndividu));
		}

		// rien à faire... à la prochaine !
		return null;
	}

	@Override
	public int getInflightCount() {
		return queue.size();
	}

}
