package ch.vd.unireg.common;

import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.registre.base.date.InstantHelper;

public class AsyncStorageWithPeriodicCleanup<K, V> extends AsyncStorage<K, V> {

	private static final Logger LOGGER = LoggerFactory.getLogger(AsyncStorageWithPeriodicCleanup.class);

	/**
	 * Timer utilisé pour le lancement régulier des tâches de cleanup
	 */
	private Timer cleanupTimer;

	/**
	 * Période de lancement des tâches de cleanup (doit être strictement positif)
	 */
	private final Duration cleanupPeriod;

	/**
	 * Nom du thread utilisé par le timer {@link #cleanupTimer}
	 */
	private final String cleanupThreadName;

	/**
	 * Nombre d'éléments affectés par les tâches de cleanup depuis le démarrage du service
	 */
	private final AtomicInteger nbPurgedElements = new AtomicInteger(0);

	/**
	 * Tâche de cleanup lancée à intervales réguliers
	 */
	public class CleanupTask extends TimerTask {

		@Override
		public final void run() {
			try {
				doInLockedEnvironment(new Action<K, V, Object>() {
					@Override
					public Object execute(Iterable<Map.Entry<K, Mutable<V>>> entries) {
						final Iterator<Map.Entry<K, Mutable<V>>> iterator = entries.iterator();
						final Instant now = InstantHelper.get();
						final Instant lastAcceptedTimestamp = now.minus(getMaximumAcceptedAge());
						while (iterator.hasNext()) {
							final Map.Entry<K, Mutable<V>> entry = iterator.next();
							final CleanupMutableObject<V> dataHolder = (CleanupMutableObject<V>) entry.getValue();
							final Instant responseArrivalTime = dataHolder.ts;
							if (responseArrivalTime.isBefore(lastAcceptedTimestamp)) {
								onPurge(entry.getKey(), dataHolder.getValue());
								iterator.remove();
							}
						}
						return null;
					}
				});
			}
			catch (Exception e) {
				LOGGER.warn("Exception envoyée par la tâche de cleanup", e);
			}
		}

		/**
		 * Surchargeable pour d'éventuels log, par exemple
		 * @param key clé de l'élément purgé
		 * @param value valeur de l'élément purgé
		 */
		protected void onPurge(K key, V value) {
			nbPurgedElements.incrementAndGet();
		}

		/**
		 * @return En millisecondes, l'age maximal accepté pour une donnée (= au delà, elle sera purgée)
		 */
		protected Duration getMaximumAcceptedAge() {
			return cleanupPeriod;
		}
	}

	public AsyncStorageWithPeriodicCleanup(int cleanupPeriodSeconds, String cleanupThreadName) {
		if (cleanupPeriodSeconds <= 0) {
			throw new IllegalArgumentException("La période de cleanup en secondes doit être strictement positive");
		}
		this.cleanupPeriod = Duration.ofSeconds(cleanupPeriodSeconds);
		this.cleanupThreadName = cleanupThreadName;
	}

	/**
	 * Sous classe de {@link MutableObject} qui maintient également un timestamp de création
	 * @param <V> le type de valeur stockée
	 */
	protected static class CleanupMutableObject<V> extends MutableObject<V> {
		public final Instant ts;
		public CleanupMutableObject(@Nullable V data) {
			super(data);
			this.ts = InstantHelper.get();
		}
	}

	@Override
	protected MutableObject<V> buildDataHolder(@Nullable V value) {
		return new CleanupMutableObject<>(value);
	}

	/**
	 * Instanciation de la tâche de cleanup
	 * @return l'instance utilisée pour les tâches de cleanup
	 */
	protected CleanupTask buildCleanupTask() {
		return new CleanupTask();
	}

	/**
	 * @return le nombre d'éléments effectivement purgés par la tache de cleanup depuis le démarrage
	 */
	public final int getNbPurgedElements() {
		return nbPurgedElements.intValue();
	}

	/**
	 * Démarre les threads annexes (cleanup)
	 */
	public void start() {
		// un thread daemon est tout-à-fait suffisant, cela ne doit en aucun cas bloquer l'arrêt de l'application
		cleanupTimer = new Timer(cleanupThreadName, true);
		cleanupTimer.schedule(buildCleanupTask(), cleanupPeriod.toMillis(), cleanupPeriod.toMillis());
	}

	/**
	 * Arrête les threads annexes (cleanup)
	 */
	public void stop() throws Exception {
		cleanupTimer.cancel();
		cleanupTimer = null;
	}
}
