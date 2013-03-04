package ch.vd.uniregctb.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public class AsyncStorageWithPeriodicCleanup<K, V> extends AsyncStorage<K, V> {

	private static final Logger LOGGER = Logger.getLogger(AsyncStorageWithPeriodicCleanup.class);

	/**
	 * Timer utilisé pour le lancement régulier des tâches de cleanup
	 */
	private Timer cleanupTimer;

	/**
	 * Période (en secondes) de lancement des tâches de cleanup (doit être strictement positif)
	 */
	private final int cleanupPeriod;

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
						final long now = TimeHelper.getPreciseCurrentTimeMillis();
						final long lastAcceptedTimestamp = now - getMaximumAcceptedAge();
						while (iterator.hasNext()) {
							final Map.Entry<K, Mutable<V>> entry = iterator.next();
							final CleanupMutableObject<V> dataHolder = (CleanupMutableObject<V>) entry.getValue();
							final long responseArrivalTs = dataHolder.ts;
							if (responseArrivalTs < lastAcceptedTimestamp) {
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
		protected long getMaximumAcceptedAge() {
			return cleanupPeriod * 1000L;
		}
	}

	public AsyncStorageWithPeriodicCleanup(int cleanupPeriodSeconds, String cleanupThreadName) {
		this.cleanupPeriod = cleanupPeriodSeconds;
		this.cleanupThreadName = cleanupThreadName;
	}

	/**
	 * Sous classe de {@link MutableObject} qui maintient également un timestamp de création
	 * @param <V> le type de valeur stockée
	 */
	protected static class CleanupMutableObject<V> extends MutableObject<V> {
		final public long ts;
		public CleanupMutableObject(@Nullable V data) {
			super(data);
			this.ts = TimeHelper.getPreciseCurrentTimeMillis();
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
		cleanupTimer = new Timer(cleanupThreadName);
		cleanupTimer.schedule(buildCleanupTask(), cleanupPeriod * 1000L, cleanupPeriod * 1000L);
	}

	/**
	 * Arrête les threads annexes (cleanup)
	 */
	public void stop() throws Exception {
		cleanupTimer.cancel();
		cleanupTimer = null;
	}
}
