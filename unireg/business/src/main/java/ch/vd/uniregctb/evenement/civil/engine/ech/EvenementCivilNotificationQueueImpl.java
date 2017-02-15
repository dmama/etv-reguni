package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;

import ch.vd.registre.base.date.InstantHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AgeTrackingBlockingQueueMixer;
import ch.vd.uniregctb.common.Aged;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchProcessingMode;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Classe utilitaire qui joue le rôle d'une queue bloquante pour le traitement des événements civils reçus de RCPers
 * <ul>
 *     <li>Trois queues d'entrées, une pour les traitements de masse, une seconde pour les traitements initiés par
 *     un utilisateur humain extérieur à Unireg, et une troisième pour les traitements initiés par un utilisateur humain
 *     interne à Unireg (= relance d'événements), les éléments postés dans ces queues sont des numéros d'individu</li>
 *     <li>Le but de ces 3 queues d'entrées est de pouvoir traiter les demandes humaines en priorité. </li>
 *     <li>En sortie, les éléments sont des listes d'événements civils (en fait, structures {@link EvenementCivilEchBasicInfo})</li>
 * </ul>
 * <p/>
 * <b>Quels sont les besoins de {@link java.util.concurrent.locks.Lock Lock} supplémentaire ?</b>
 * <p/>
 * Et bien voilà... je m'étais dit au départ que les méthodes {@link #post(Long, EvenementCivilEchProcessingMode) post()} devrait faire en sorte d'éliminer les doublons de numéros d'individu...
 * Dans ce cadre, afin de bien les filtrer, il était important de faire en sorte que deux appels à {@link BlockingQueue#add} ne devaient
 * pas être faits en même temps, d'où l'utilisation d'un {@link java.util.concurrent.locks.ReentrantLock ReentrantLock}.
 * <p/>
 * Mais du coup, il était assez raisonnable de penser que la méthode {@link BlockingQueue#poll} devait subir le même genre de contrainte. Et si
 * c'est bien le cas, alors cela signifie qu'aucun appel à {@link #post(Long, EvenementCivilEchProcessingMode) post()} ne pourrait être fait pendant que la méthode {@link #poll(long, java.util.concurrent.TimeUnit) poll}
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
 * je vois l'implémentation de l'appelant d'une méthode {@link #post(Long, EvenementCivilEchProcessingMode) post()}) que <i>le nouvel événement est déjà committé en base</i> au moment
 * où l'appel à une méthode {@link #post(Long, EvenementCivilEchProcessingMode) post()} est lancé, alors le thread qui est justement en train de récupérer les événements civils de cet
 * individu va de toute façon également récupérer cet événement-là.
 * <p/>
 * En revanche, si on choisit le second cas, alors on perd l'élimination des doublons, et on risque relativement souvent de voir la méthode
 * {@link #poll(long, java.util.concurrent.TimeUnit) poll} faire une requête en base dans le vide (car les événements auraient déjà été traités par
 * le passage précédent de la valeur en doublon). Notons bien que ce cas n'est pas totalement exclu dans la première solution, dans le
 * cas où l'identifiant de l'individu est enlevé de la queue entre le moment où l'événenement correspondant est effectivement committé en base
 * et le moment où la méthode {@link #post(Long, EvenementCivilEchProcessingMode) post()} vérifie sa présence... mais cela devrait se produire moins souvent.
 */
 public class EvenementCivilNotificationQueueImpl implements EvenementCivilNotificationQueue, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilNotificationQueueImpl.class);

	private final BlockingQueue<DelayedIndividu> batchQueue = new DelayQueue<>();
	private final BlockingQueue<DelayedIndividu> manualQueue = new DelayQueue<>();
	private final BlockingQueue<DelayedIndividu> immediateQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<DelayedIndividu> finalQueue = new SynchronousQueue<>(true);
	private final AgeTrackingBlockingQueueMixer<DelayedIndividu> mixer;
	private final ReentrantLock lock = new ReentrantLock();

	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchService evtCivilService;
	private final Duration delay;

	public EvenementCivilNotificationQueueImpl(int delayInSeconds) {
		if (delayInSeconds < 0) {
			throw new IllegalArgumentException("delay should not be negative!");
		}
		LOGGER.info(String.format("Traitement des événements civils e-CH artificiellement décalé de %d seconde%s.", delayInSeconds, delayInSeconds > 1 ? "s" : ""));
		delay = Duration.ofSeconds(delayInSeconds);

		final List<BlockingQueue<DelayedIndividu>> input = new ArrayList<>(3);
		input.add(immediateQueue);
		input.add(manualQueue);
		input.add(batchQueue);
		mixer = new AgeTrackingBlockingQueueMixer<>(input, finalQueue, 5, 30);  // 5 minutes en 30 intervales -> intervales de 10 secondes
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtCivilService(EvenementCivilEchService evtCivilService) {
		this.evtCivilService = evtCivilService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		mixer.start("EvtCivilEchMixer");
	}

	@Override
	public void destroy() throws Exception {
		mixer.stop();
	}

	private class DelayedIndividu implements Delayed, Aged {

		private final long noIndividu;
		private final Instant creation = InstantHelper.get();
		private final Instant expiration = creation.plus(delay);

		public DelayedIndividu(long noIndividu) {
			this.noIndividu = noIndividu;
		}

		private long getDelay(TimeUnit unit, Instant now) {
			return unit.convert(Duration.between(now, expiration).toNanos(), TimeUnit.NANOSECONDS);
		}

		@Override
		public long getDelay(@NotNull TimeUnit unit) {
			return getDelay(unit, InstantHelper.get());
		}

		@Override
		public int compareTo(@NotNull Delayed o) {
			final Instant now = InstantHelper.get();
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

		@Override
		public String toString() {
			return "Individu{no=" + noIndividu + '}';
		}

		@Override
		public Duration getAge() {
			return Duration.between(creation, InstantHelper.get());
		}
	}

	@Override
	public void post(Long noIndividu, EvenementCivilEchProcessingMode mode) {
		lock.lock();
		try {
			internalPost(noIndividu, mode);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void postAll(Collection<Long> nosIndividus) {
		if (nosIndividus != null && !nosIndividus.isEmpty()) {
			lock.lock();
			try {
				// élimination des doublons et découplage des collections (en cas de manipulation - sur le thread de traitement, par exemple - pendant l'insertion)
				// tout en conservant l'ordre initial (pour les tests)
				for (Long noIndividu : new LinkedHashSet<>(nosIndividus)) {
					internalPost(noIndividu, EvenementCivilEchProcessingMode.BATCH);
				}
			}
			finally {
				lock.unlock();
			}
		}
	}

	/**
	 * L'appelant doit impérativement posséder le verrou
	 * @param noIndividu numéro d'individu à poster dans la queue interne
	 * @param mode le mode de traitement
	 */
	private void internalPost(Long noIndividu, EvenementCivilEchProcessingMode mode) {
		Assert.isTrue(lock.isHeldByCurrentThread());

		if (noIndividu == null) {
			throw new NullPointerException("noIndividu");
		}

		final DelayedIndividu elt = new DelayedIndividu(noIndividu);
		final boolean wasBatch = batchQueue.remove(elt);
		final boolean wasManual = manualQueue.remove(elt);
		final boolean wasImmediate = immediateQueue.remove(elt);
		final boolean wasNowhere = !wasBatch && !wasManual && !wasImmediate;
		Assert.isTrue((wasBatch ? 1 : 0) + (wasManual ? 1 : 0) + (wasImmediate ? 1 : 0) <= 1);

		final BlockingQueue<DelayedIndividu> postingQueue;
		switch (mode) {
			case BATCH:
				if (wasNowhere || wasBatch) {
					postingQueue = batchQueue;
				}
				else {
					postingQueue = manualQueue;
				}
				break;
			case MANUAL:
				postingQueue = manualQueue;
				break;
			case IMMEDIATE:
				if (wasNowhere || wasImmediate) {
					postingQueue = immediateQueue;
				}
				else {
					postingQueue = manualQueue;
				}
				break;
			default:
				throw new IllegalArgumentException("Invalid mode : " + mode);
		}

		postingQueue.add(elt);
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
	protected List<EvenementCivilEchBasicInfo> buildLotsEvenementsCivils(final long noIndividu) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		return template.execute(status -> evtCivilService.buildLotEvenementsCivilsNonTraites(noIndividu));
	}

	@Override
	public int getTotalCount() {
		return mixer.size();
	}

	@Override
	public int getInBatchQueueCount() {
		return batchQueue.size();
	}

	@Override
	public Long getBatchQueueSlidingAverageAge() {
		return mixer.getSlidingAverageAge(batchQueue);
	}

	@Override
	public Long getBatchQueueGlobalAverageAge() {
		return mixer.getGlobalAverageAge(batchQueue);
	}

	@Override
	public int getInManualQueueCount() {
		return manualQueue.size();
	}

	@Override
	public Long getManualQueueSlidingAverageAge() {
		return mixer.getSlidingAverageAge(manualQueue);
	}

	@Override
	public Long getManualQueueGlobalAverageAge() {
		return mixer.getGlobalAverageAge(manualQueue);
	}

	@Override
	public int getInImmediateQueueCount() {
		return immediateQueue.size();
	}

	@Override
	public Long getImmediateQueueSlidingAverageAge() {
		return mixer.getSlidingAverageAge(immediateQueue);
	}

	@Override
	public Long getImmediateQueueGlobalAverageAge() {
		return mixer.getGlobalAverageAge(immediateQueue);
	}

	@Override
	public int getInFinalQueueCount() {
		return finalQueue.size();
	}

	@Override
	public int getInHatchesCount() {
		return mixer.sizeInTransit();
	}
}
