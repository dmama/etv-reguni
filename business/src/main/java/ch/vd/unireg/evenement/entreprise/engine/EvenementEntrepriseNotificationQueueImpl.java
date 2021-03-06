package ch.vd.unireg.evenement.entreprise.engine;

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
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.InstantHelper;
import ch.vd.unireg.common.AgeTrackingBlockingQueueMixer;
import ch.vd.unireg.common.Aged;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseProcessingMode;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseService;

/**
 * Classe utilitaire qui joue le rôle d'une queue bloquante pour le traitement des événements entreprise reçus de RCEnt.
 * <p>[Note: cette  classe est reprise est adaptée de son équivalent et modèle traitant des événements civils eCH, y compris les commentaires, révisés en conséquance]</p>
 * <ul>
 *     <li>Trois queues d'entrées, une pour les traitements de masse, deux pour les traitements initiés par un utilisateur humain
 *     interne à Unireg (= relance d'événements), selon qu'ils étaient déjà en attente ou non</li>
 *     <li>Les éléments postés dans ces queues sont des numéros d'entreprise</li>
 *     <li>Le but de ces deux queues d'entrées est de pouvoir traiter les demandes humaines en priorité. </li>
 *     <li>En sortie, les éléments sont des listes d'événements entreprise (en fait, structures {@link EvenementEntrepriseBasicInfo})</li>
 * </ul>
 * <p/>
 * <b>Quels sont les besoins de {@link java.util.concurrent.locks.Lock Lock} supplémentaire ?</b>
 * <p/>
 * Et bien voilà... je m'étais dit au départ que les méthodes {@link #post(Long, EvenementEntrepriseProcessingMode) post()} devrait faire en sorte d'éliminer les doublons de numéros d'entreprise...
 * Dans ce cadre, afin de bien les filtrer, il était important de faire en sorte que deux appels à {@link BlockingQueue#add} ne devaient
 * pas être faits en même temps, d'où l'utilisation d'un {@link ReentrantLock ReentrantLock}.
 * <p/>
 * Mais du coup, il était assez raisonnable de penser que la méthode {@link BlockingQueue#poll} devait subir le même genre de contrainte. Et si
 * c'est bien le cas, alors cela signifie qu'aucun appel à {@link #post(Long, EvenementEntrepriseProcessingMode) post()} ne pourrait être fait pendant que la méthode {@link #poll(java.time.Duration) poll}
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
 * je vois l'implémentation de l'appelant d'une méthode {@link #post(Long, EvenementEntrepriseProcessingMode) post()}) que <i>le nouvel événement est déjà committé en base</i> au moment
 * où l'appel à une méthode {@link #post(Long, EvenementEntrepriseProcessingMode) post()} est lancé, alors le thread qui est justement en train de récupérer les événements de cette
 * entreprise va de toute façon également récupérer cet événement-là.
 * <p/>
 * En revanche, si on choisit le second cas, alors on perd l'élimination des doublons, et on risque relativement souvent de voir la méthode
 * {@link #poll(java.time.Duration) poll} faire une requête en base dans le vide (car les événements auraient déjà été traités par
 * le passage précédent de la valeur en doublon). Notons bien que ce cas n'est pas totalement exclu dans la première solution, dans le
 * cas où l'identifiant de l'entreprise est enlevée de la queue entre le moment où l'événenement correspondant est effectivement committé en base
 * et le moment où la méthode {@link #post(Long, EvenementEntrepriseProcessingMode) post()} vérifie sa présence... mais cela devrait se produire moins souvent.
 */
 public class EvenementEntrepriseNotificationQueueImpl implements EvenementEntrepriseNotificationQueue, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEntrepriseNotificationQueueImpl.class);

	private final BlockingQueue<DelayedEntreprise> bulkQueue = new DelayQueue<>();
	private final BlockingQueue<DelayedEntreprise> priorityQueue = new DelayQueue<>();
	private final BlockingQueue<DelayedEntreprise> immediateQueue = new LinkedBlockingQueue<>();
	private final BlockingQueue<DelayedEntreprise> finalQueue = new SynchronousQueue<>(true);
	private final AgeTrackingBlockingQueueMixer<DelayedEntreprise> mixer;
	private final ReentrantLock lock = new ReentrantLock();

	private PlatformTransactionManager transactionManager;
	private EvenementEntrepriseService evtEntrepriseService;
	private final Duration delay;

	public EvenementEntrepriseNotificationQueueImpl(int delayInSeconds) {
		if (delayInSeconds < 0) {
			throw new IllegalArgumentException("delay should not be negative!");
		}
		LOGGER.info(String.format("Traitement des événements entreprise artificiellement décalé de %d seconde%s.", delayInSeconds, delayInSeconds > 1 ? "s" : ""));
		delay = Duration.ofSeconds(delayInSeconds);

		final List<BlockingQueue<DelayedEntreprise>> input = new ArrayList<>(2);
		input.add(immediateQueue);
		input.add(bulkQueue);
		input.add(priorityQueue);
		mixer = new AgeTrackingBlockingQueueMixer<>(input, finalQueue, 5, 30);  // 5 minutes en 30 intervales -> intervales de 10 secondes
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtEntrepriseService(EvenementEntrepriseService evtEntrepriseService) {
		this.evtEntrepriseService = evtEntrepriseService;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		mixer.start("EvtEntrepriseMixer");
	}

	@Override
	public void destroy() throws Exception {
		mixer.stop();
	}

	private class DelayedEntreprise implements Delayed, Aged {

		private final long noEntrepriseCivile;
		private final Instant creation = InstantHelper.get();
		private final Instant expiration = creation.plus(delay);

		public DelayedEntreprise(long noEntrepriseCivile) {
			this.noEntrepriseCivile = noEntrepriseCivile;
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
			final long yourDelay = ((DelayedEntreprise) o).getDelay(TimeUnit.NANOSECONDS, now);
			return Long.compare(myDelay, yourDelay);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final DelayedEntreprise that = (DelayedEntreprise) o;
			return noEntrepriseCivile == that.noEntrepriseCivile;
		}

		@Override
		public int hashCode() {
			return (int) (noEntrepriseCivile ^ (noEntrepriseCivile >>> 32));
		}

		@Override
		public String toString() {
			return "EntrepriseCivile{no=" + noEntrepriseCivile + '}';
		}

		@Override
		public Duration getAge() {
			return Duration.between(creation, InstantHelper.get());
		}
	}

	@Override
	public void post(Long noEntrepriseCivile, EvenementEntrepriseProcessingMode mode) {
		lock.lock();
		try {
			internalPost(noEntrepriseCivile, mode);
		}
		finally {
			lock.unlock();
		}
	}

	@Override
	public void postAll(Collection<Long> nosEntreprisesCiviles) {
		if (nosEntreprisesCiviles != null && !nosEntreprisesCiviles.isEmpty()) {
			lock.lock();
			try {
				// élimination des doublons et découplage des collections (en cas de manipulation - sur le thread de traitement, par exemple - pendant l'insertion)
				// tout en conservant l'ordre initial (pour les tests)
				for (Long noEntrepriseCivile : new LinkedHashSet<>(nosEntreprisesCiviles)) {
					internalPost(noEntrepriseCivile, EvenementEntrepriseProcessingMode.BULK);
				}
			}
			finally {
				lock.unlock();
			}
		}
	}

	/**
	 * L'appelant doit impérativement posséder le verrou
	 * @param noEntrepriseCivile numéro d'entreprise à poster dans la queue interne
	 * @param mode le mode de traitement
	 */
	private void internalPost(Long noEntrepriseCivile, EvenementEntrepriseProcessingMode mode) {
		if (!lock.isHeldByCurrentThread()) {
			throw new IllegalStateException();
		}

		if (noEntrepriseCivile == null) {
			throw new NullPointerException("noEntrepriseCivile");
		}

		final DelayedEntreprise elt = new DelayedEntreprise(noEntrepriseCivile);
		final boolean wasBulk = bulkQueue.remove(elt);
		final boolean wasPriority = priorityQueue.remove(elt);
		final boolean wasImmediate = immediateQueue.remove(elt);
		final boolean wasNowhere = !wasBulk && !wasPriority && !wasImmediate;
		if ((wasBulk ? 1 : 0) + (wasPriority ? 1 : 0) + (wasImmediate ? 1 : 0) > 1) {
			throw new IllegalArgumentException();
		}

		final BlockingQueue<DelayedEntreprise> postingQueue;
		switch (mode) {
		case BULK:
			if (wasNowhere || wasBulk) {
				postingQueue = bulkQueue;
			}
			else {
				postingQueue = priorityQueue;
			}
			break;
		case PRIORITY:
			postingQueue = priorityQueue;
			break;
		case IMMEDIATE:
			if (wasNowhere || wasImmediate) {
				postingQueue = immediateQueue;
			}
			else {
				postingQueue = priorityQueue;
			}
			break;
		default:
			throw new IllegalArgumentException("Invalid mode : " + mode);
		}

		postingQueue.add(elt);
	}

	@Override
	public Batch poll(Duration timeout) throws InterruptedException {
		if (timeout.isZero() || timeout.isNegative()) {
			throw new IllegalArgumentException("timeout should be positive");
		}
		final DelayedEntreprise elt = finalQueue.poll(timeout.toNanos(), TimeUnit.NANOSECONDS);
		if (elt != null) {
			// 1. trouve tous les événements de cette entreprise qui sont dans un état A_TRAITER, EN_ATTENTE, EN_ERREUR
			// 2. tri de ces événements par date, puis ordre arrivée
			return new Batch(elt.noEntrepriseCivile, buildLotsEvenementsEntreprises(elt.noEntrepriseCivile));
		}

		// rien à faire... à la prochaine !
		return null;
	}

	@Nullable
	protected List<EvenementEntrepriseBasicInfo> buildLotsEvenementsEntreprises(final long noEntrepriseCivile) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		template.setReadOnly(true);
		return template.execute(status -> evtEntrepriseService.buildLotEvenementsEntrepriseNonTraites(noEntrepriseCivile));
	}

	@Override
	public int getTotalCount() {
		return mixer.size();
	}

	@Override
	public int getInBulkQueueCount() {
		return bulkQueue.size();
	}

	@Override
	public Long getBulkQueueSlidingAverageAge() {
		return mixer.getSlidingAverageAge(bulkQueue);
	}

	@Override
	public Long getBulkQueueGlobalAverageAge() {
		return mixer.getGlobalAverageAge(bulkQueue);
	}

	@Override
	public int getInPriorityQueueCount() {
		return priorityQueue.size();
	}

	@Override
	public Long getPriorityQueueSlidingAverageAge() {
		return mixer.getSlidingAverageAge(priorityQueue);
	}

	@Override
	public Long getPriorityQueueGlobalAverageAge() {
		return mixer.getGlobalAverageAge(priorityQueue);
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
