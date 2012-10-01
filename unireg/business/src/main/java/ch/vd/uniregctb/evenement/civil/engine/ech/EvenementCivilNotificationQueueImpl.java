package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

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
 * et le moment où la méthode {@link #post(Long, boolean) post} vérifie sa présence... mais cela devrait se produire moins souvent.
 */
@SuppressWarnings("JavadocReference") // TODO FRED M-à-J Javadoc et suppression du suppressWarning
public class EvenementCivilNotificationQueueImpl implements EvenementCivilNotificationQueue, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilNotificationQueueImpl.class);

	private final BlockingQueue<DelayedIndividu> batchQueue = new DelayQueue<DelayedIndividu>();
	private final BlockingQueue<DelayedIndividu> manualQueue = new DelayQueue<DelayedIndividu>();
	private final BlockingQueue<DelayedIndividu> finalQueue = new ArrayBlockingQueue<DelayedIndividu>(20);
	private final ReentrantLock batchLock = new ReentrantLock();
	private final ReentrantLock manualLock = new ReentrantLock();
	private final AtomicInteger totalInHatches = new AtomicInteger(0);

	private final ServingHatch batchHatch = new ServingHatch("batchHatch", batchQueue, totalInHatches, finalQueue);
	private final ServingHatch manualHatch = new ServingHatch("manualHatch", manualQueue, totalInHatches, finalQueue);

	private EvenementCivilEchService evtCivilService;
	private final long delayNs;

	public EvenementCivilNotificationQueueImpl(int delayInSeconds) {
		if (delayInSeconds < 0) {
			throw new IllegalArgumentException("delay should not be negative!");
		}
		LOGGER.info(String.format("Traitement des événements civils e-CH artificiellement décalé de %d seconde%s.", delayInSeconds, delayInSeconds > 1 ? "s" : ""));
		delayNs = TimeUnit.SECONDS.toNanos(delayInSeconds);
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtCivilService(EvenementCivilEchService evtCivilService) {
		this.evtCivilService = evtCivilService;
	}

	private static long getTimestamp() {
		return System.nanoTime();
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		manualHatch.start();
		batchHatch.start();
	}

	@Override
	public void destroy() throws Exception {
		if (manualHatch != null) {
			manualHatch.stopServing();
		}
		if (batchHatch != null) {
			batchHatch.stopServing();
		}
	}

	private class DelayedIndividu implements Delayed {

		private final long noIndividu;
		private final long startTimestamp;

		public DelayedIndividu(long noIndividu, long delayOffset) {
			this.noIndividu = noIndividu;
			this.startTimestamp = getTimestamp() + delayOffset;
		}

		private long getDelay(TimeUnit unit, long nowNanos) {
			return unit.convert(startTimestamp + delayNs - nowNanos, TimeUnit.NANOSECONDS);
		}

		@Override
		public long getDelay(TimeUnit unit) {
			return getDelay(unit, getTimestamp());
		}

		@Override
		public int compareTo(Delayed o) {
			final long now = getTimestamp();
			final long myDelay = getDelay(TimeUnit.NANOSECONDS, now);
			final long yourDelay = ((DelayedIndividu) o).getDelay(TimeUnit.NANOSECONDS, now);
			return myDelay < yourDelay ? -1 : (myDelay > yourDelay ? 1 : 0);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final DelayedIndividu that = (DelayedIndividu) o;
			return noIndividu == that.noIndividu;
		}

		@Override
		public int hashCode() {
			return (int) (noIndividu ^ (noIndividu >>> 32));
		}
	}

	@Override
	public void postBatch(Long noIndividu, boolean immediate) {
		batchLock.lock();
		try {
			internalPost(noIndividu, immediate, batchQueue);
		}
		finally {
			batchLock.unlock();
		}
	}

	@Override
	public void postManual(Long noIndividu, boolean immediate) {
		manualLock.lock();
		try {
			internalPost(noIndividu, immediate, manualQueue);
		}
		finally {
			manualLock.unlock();
		}
	}

	@Override
	public void postAll(Collection<Long> nosIndividus) {
		if (nosIndividus != null && nosIndividus.size() > 0) {
			batchLock.lock();
			try {
				for (Long noIndividu : nosIndividus) {
					internalPost(noIndividu, false, batchQueue);
				}
			}
			finally {
				batchLock.unlock();
			}
		}
	}

	/**
	 * L'appelant doit impérativement possédé le verrou associé à la queue passée en paramétre
	 * @param noIndividu numéro d'individu à poster dans la queue interne
	 * @param queue la queue ou poster, manuel ou batch
	 */
	private void internalPost(Long noIndividu, boolean immediate, BlockingQueue<DelayedIndividu> queue) {
		Assert.isTrue(manualLock.isHeldByCurrentThread() && queue == manualQueue || batchLock.isHeldByCurrentThread() && queue == batchQueue);

		if (noIndividu == null) {
			throw new NullPointerException("noIndividu");
		}

		// on replace l'élément en fin de queue s'il était déjà présent (de telle sorte qu'on attende toujours au minimum le délai prévu
		// compté depuis le dernier événements reçu pour un individu)
		final DelayedIndividu elt = new DelayedIndividu(noIndividu, immediate ? -delayNs : 0L);
		if (immediate) {
			// on ne veut pas accélérer le traitement d'un individu déjà en attente (pour s'assurer que, le cas échéant, le service civil
			// vu au travers de son web-service est bien à jour, voir SIREF-2016)
			if (!queue.contains(elt) && !finalQueue.contains(elt)) {
				queue.add(elt);
			}
		}
		else {
			queue.remove(elt);
			finalQueue.remove(elt);
			queue.add(elt);
		}
	}

	@Override
	public Batch poll(long timeout, TimeUnit unit) throws InterruptedException {
		final DelayedIndividu elt = finalQueue.poll(timeout, unit);
		if (elt != null) {
			// 1. trouve tous les événements civils de cet individu qui sont dans un état A_TRAITER, EN_ATTENTE, EN_ERREUR
			// 2. tri de ces événements par date, puis type d'événement
			return new Batch(elt.noIndividu, buildLotsEvenementsCivils(elt.noIndividu));
		}

		// rien à faire... à la prochaine !
		return null;
	}

	@Nullable
	protected List<EvenementCivilEchBasicInfo> buildLotsEvenementsCivils(long noIndividu) {
		return evtCivilService.buildLotEvenementsCivils(noIndividu);
	}

	@Override
	public int getInflightCount() {
		return batchQueue.size() + manualQueue.size() + finalQueue.size() + totalInHatches.get();
	}

	private static class ServingHatch extends Thread {

		private static final int HATCH_TIMEOUT = 100; // ms

		private BlockingQueue<DelayedIndividu> queue;
		private BlockingQueue<DelayedIndividu> finalQueue;
		private AtomicInteger totalInHatches;
		private boolean stopped = false;


		private ServingHatch(String threadName, BlockingQueue<DelayedIndividu> queue, AtomicInteger totalInHatches, BlockingQueue<DelayedIndividu> finalQueue) {
			super(threadName);
			this.queue = queue;
			this.totalInHatches = totalInHatches;
			this.finalQueue = finalQueue;
		}

		@Override
		public void run() {
			LOGGER.info("Démarrage du thread: " + Thread.currentThread().getName());

			// On fait le passe-plat de la queue d'origine à la queue finale
			DelayedIndividu evt = null;
			while (!stopped) {
				try {
					if (evt == null) {
						evt = queue.poll(HATCH_TIMEOUT, TimeUnit.MILLISECONDS);
					}
					if (evt != null) {
						totalInHatches.incrementAndGet();
						boolean offerAccepted = finalQueue.offer(evt, HATCH_TIMEOUT, TimeUnit.MILLISECONDS);
						if (offerAccepted) {
							evt = null;
							totalInHatches.decrementAndGet();
						}
					}
				}
				catch (InterruptedException e) {
					LOGGER.error(Thread.currentThread().getName() + " interrompu", e);
					stopServing();
				}
			}
		}

		public void stopServing() {
			stopped = true;
		}

	}
}
