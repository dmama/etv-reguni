package ch.vd.uniregctb.common;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jetbrains.annotations.NotNull;

/**
 * Mélangeur de {@link BlockingQueue}'s qui maintient une statistique sur l'âge des éléments au moment où ils passent dans la queue 'output'
 * en fonction de la queue d'entrée
 * @param <T> la classe des éléments en transit dans les queues
 */
public class AgeTrackingBlockingQueueMixer<T extends Aged> extends BlockingQueueMixer<T> {

	/**
	 * Les données collectées par queue d'entrée
	 */
	private final Map<IdentityKey<BlockingQueue<T>>, QueueData> queueMap;

	/**
	 * Durée (en millisecondes) de collecte des informations dans un slot
	 */
	private final long slotDuration;

	private Timer tickingTimer;

	/**
	 * @param inputQueues une liste de queues d'entrée
	 * @param outputQueue la queue de sortie
	 * @param spotAveragingWindow en minutes, la durée de la période de calcul des moyennes glissantes
	 * @param nbAveragingSlots le nombre de slots (qui recouvrent donc l'intégralité de la fenêtre de calcul), <i>a priori</i> au moins 2
	 */
	public AgeTrackingBlockingQueueMixer(List<BlockingQueue<T>> inputQueues, BlockingQueue<T> outputQueue, int spotAveragingWindow, int nbAveragingSlots) {
		super(inputQueues, outputQueue);

		if (nbAveragingSlots <= 1) {
			throw new IllegalArgumentException("Le nombre de slots devrait être au moins 2");
		}
		if (spotAveragingWindow <= 0) {
			throw new IllegalArgumentException("La fenêtre de calcul des moyennes doit être d'au moins une minute");
		}

		this.slotDuration = TimeUnit.MINUTES.toMillis(spotAveragingWindow) / nbAveragingSlots;
		if (this.slotDuration < 1000L) {
			throw new IllegalArgumentException("Un slot de calcul de la moyenne glissante ne devrait pas être plus petit qu'une seconde...");
		}

		// on prépare la map pour le plus devoir synchroniser dessus par la suite
		final Map<IdentityKey<BlockingQueue<T>>, QueueData> map = new HashMap<>(inputQueues.size());
		for (BlockingQueue<T> inputQueue : inputQueues) {
			final IdentityKey<BlockingQueue<T>> key = new IdentityKey<>(inputQueue);
			map.put(key, new QueueData(nbAveragingSlots));
		}

		// ... et pour ne pas faire de bêtise, on met la map en read-only
		this.queueMap = Collections.unmodifiableMap(map);
	}

	@Override
	public void start(ThreadNameGenerator threadNameGenerator) {
		super.start(threadNameGenerator);
		tickingTimer = new Timer("Tick-" + threadNameGenerator.getNewThreadName());
		tickingTimer.schedule(new TickingTask(), slotDuration, slotDuration);
	}

	@Override
	public void stop() {
		if (tickingTimer != null) {
			tickingTimer.cancel();
			tickingTimer = null;
		}
		reset();
		super.stop();
	}

	/**
	 * Opération à effectuer toutes les minutes pour assurer un calcul de la moyenne sur quelques minutes seulement
	 */
	private class TickingTask extends TimerTask {
		@Override
		public void run() {
			onClockChimes();
		}
	}

	/**
	 * Une minute vient de sonner : on oublie tout ce qui est trop vieux et on recommence à compter
	 * pour la minute qui commence...
	 */
	protected void onClockChimes() {
		for (QueueData data : queueMap.values()) {
			data.onClockChimes();
		}
	}

	protected void reset() {
		for (QueueData data : queueMap.values()) {
			data.reset();
		}
	}

	/**
	 * @param element élément qui vient de passer d'une queue d'entrée à la queue de sortie
	 * @param fromQueue la queue d'entrée que l'élément vient de quitter
	 * @throws IllegalArgumentException si la queue ne fait pas partie des queues d'entrées connues
	 */
	@Override
	protected void onElementOffered(T element, BlockingQueue<T> fromQueue) {
		super.onElementOffered(element, fromQueue);

		final Duration age = element.getAge();
		final QueueData data = getQueueData(fromQueue);
		data.incomingElement(age);
	}

	/**
	 * @param inputQueue la queue d'entrée visée
	 * @return l'âge moyen, dans les dernières minutes, des éléments passés dans la queue finale depuis cette queue d'entrée, en millisecondes
	 * (<code>null</code> si aucun élément n'est passé par cette queue dans les dernières minutes)
	 * @throws IllegalArgumentException si la queue indiquée ne fait pas partie des queues d'entrée connues
	 */
	public Long getSlidingAverageAge(BlockingQueue<T> inputQueue) {
		final QueueData data = getQueueData(inputQueue);
		final Long avgAgeMicrosecs = data.getSlidingAverageAge();
		return avgAgeMicrosecs != null ? TimeUnit.MICROSECONDS.toMillis(avgAgeMicrosecs) : null;
	}

	/**
	 * @param inputQueue la queue d'entrée visée
	 * @return l'âge moyen, depuis le démarrage du service, des éléments passés dans la queue finale depuis cette queue d'entrée, en millisecondes
	 * (<code>null</code> si aucun élément n'est jamais passé par cette queue
	 * @throws IllegalArgumentException si la queue indiquée ne fait pas partie des queues d'entrée connues
	 */
	public Long getGlobalAverageAge(BlockingQueue<T> inputQueue) {
		final QueueData data = getQueueData(inputQueue);
		final Long avgAgeMicrosecs = data.getGlobalAverageAge();
		return avgAgeMicrosecs != null ? TimeUnit.MICROSECONDS.toMillis(avgAgeMicrosecs) : null;

	}

	@NotNull
	private QueueData getQueueData(BlockingQueue<T> inputQueue) {
		final IdentityKey<BlockingQueue<T>> key = new IdentityKey<>(inputQueue);
		final QueueData data = queueMap.get(key);
		if (data == null) {
			throw new IllegalArgumentException("Unknown input queue");
		}
		return data;
	}

	/**
	 * Données conservées pour une queue d'entrée : quelques minutes de mesures
	 */
	private static class QueueData {
		
		private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
		private final int[] nbSlidingElements;
		private final long[] sumOfSlidingAges;
		private int totalNbElements = 0;
		private long totalSumOfAges = 0L;
		private int currentSlot = 0;
		private int highWaterSlotNumber = 0;

		private QueueData(int nbSlots) {
			nbSlidingElements = new int[nbSlots];
			sumOfSlidingAges = new long[nbSlots];
		}

		private interface Action<T> {
			T run();
		}

		private interface LockAction<T> extends Action<T> {
			boolean withWriteLock();
		}

		private static <T> T doInLock(Lock lock, Action<T> action) {
			lock.lock();
			try {
				return action.run();
			}
			finally {
				lock.unlock();
			}
		}

		private <T> T doInLock(LockAction<T> lockAction) {
			return doInLock(lockAction.withWriteLock() ? rwLock.writeLock() : rwLock.readLock(), lockAction);
		}

		private abstract static class WriteLockAction<T> implements LockAction<T> {
			@Override
			public final boolean withWriteLock() {
				return true;
			}
		}

		private abstract static class ReadLockAction<T> implements LockAction<T> {
			@Override
			public final boolean withWriteLock() {
				return false;
			}
		}

		private final LockAction<?> DONG = new WriteLockAction<Object>() {
			@Override
			public Object run() {
				currentSlot = (currentSlot + 1) % nbSlidingElements.length;
				if (highWaterSlotNumber < currentSlot) {
					highWaterSlotNumber = currentSlot;
				}
				nbSlidingElements[currentSlot] = 0;
				sumOfSlidingAges[currentSlot] = 0L;
				return null;
			}
		};

		private final LockAction<Long> SLIDING_AVG = new ReadLockAction<Long>() {
			@Override
			public Long run() {
				int totalNbElements = 0;
				long totalSumOfAges = 0L;
				for (int i = 0 ; i <= highWaterSlotNumber ; ++ i) {
					totalNbElements += nbSlidingElements[i];
					totalSumOfAges += sumOfSlidingAges[i];
				}
				return totalNbElements > 0 ? totalSumOfAges / totalNbElements : null;
			}
		};

		private final LockAction<Long> GLOBAL_AVG = new ReadLockAction<Long>() {
			@Override
			public Long run() {
				return totalNbElements > 0 ? totalSumOfAges / totalNbElements : null;
			}
		};

		private final LockAction<?> RESET = new WriteLockAction<Object>() {
			@Override
			public Object run() {
				Arrays.fill(nbSlidingElements, 0);
				Arrays.fill(sumOfSlidingAges, 0L);
				totalNbElements = 0;
				totalSumOfAges = 0L;
				currentSlot = 0;
				highWaterSlotNumber = 0;
				return null;
			}
		};

		public void incomingElement(final Duration age) {
			doInLock(new WriteLockAction<Object>() {
				@Override
				public Object run() {
					final long micros = TimeUnit.NANOSECONDS.toMicros(age.toNanos());

					// moyenne glissante
					++nbSlidingElements[currentSlot];
					sumOfSlidingAges[currentSlot] += micros;

					// moyenne générale
					++totalNbElements;
					totalSumOfAges += micros;

					return null;
				}
			});
		}

		public void onClockChimes() {
			doInLock(DONG);
		}

		public Long getSlidingAverageAge() {
			return doInLock(SLIDING_AVG);
		}

		public Long getGlobalAverageAge() {
			return doInLock(GLOBAL_AVG);
		}

		public void reset() {
			doInLock(RESET);
		}
	}
}
