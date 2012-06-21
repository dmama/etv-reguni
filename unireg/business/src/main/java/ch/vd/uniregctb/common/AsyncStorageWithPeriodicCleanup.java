package ch.vd.uniregctb.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.jetbrains.annotations.Nullable;

public class AsyncStorageWithPeriodicCleanup<K, V> extends AsyncStorage<K, V> {

	private Timer cleanupTimer;

	private final int cleanupPeriod;
	private final String cleanupThreadName;

	public class CleanupTask extends TimerTask {

		@Override
		public final void run() {
			synchronized (map) {
				final Iterator<Map.Entry<K,DataHolder<V>>> iterator = map.entrySet().iterator();
				final long now = TimeHelper.getPreciseCurrentTimeMillis();
				final long lastAcceptedTimestamp = now - getMaximumAcceptedAge();
				while (iterator.hasNext()) {
					final Map.Entry<K, DataHolder<V>> entry = iterator.next();
					final CleanupDataHolder<V> dataHolder = (CleanupDataHolder<V>) entry.getValue();
					final long responseArrivalTs = dataHolder.ts;
					if (responseArrivalTs < lastAcceptedTimestamp) {
						onPurge(entry.getKey(), dataHolder.data);
						iterator.remove();
					}
				}
			}
		}

		protected void onPurge(K key, V value) {
			// pour le log, éventuellement...
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

	protected static class CleanupDataHolder<V> extends DataHolder<V> {
		final public long ts;
		public CleanupDataHolder(@Nullable V data) {
			super(data);
			this.ts = TimeHelper.getPreciseCurrentTimeMillis();
		}
	}

	@Override
	protected DataHolder<V> buildDataHolder(@Nullable V value) {
		return new CleanupDataHolder<V>(value);
	}

	protected CleanupTask buildCleanupTask() {
		return new CleanupTask();
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
