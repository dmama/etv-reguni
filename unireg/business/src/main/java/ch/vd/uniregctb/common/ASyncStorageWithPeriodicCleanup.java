package ch.vd.uniregctb.common;

import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.jetbrains.annotations.Nullable;

public class ASyncStorageWithPeriodicCleanup<K, V> extends ASyncStorage<K, V> {

	private Timer cleanupTimer;
	private int cleanupPeriod;

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

	public ASyncStorageWithPeriodicCleanup(int cleanupPeriodSeconds) {
		this.cleanupPeriod = cleanupPeriodSeconds;
	}

	protected static class CleanupDataHolder<V> extends DataHolder<V> {
		final public long ts;
		public CleanupDataHolder(@Nullable V data) {
			super(data);
			ts = TimeHelper.getPreciseCurrentTimeMillis();
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
	 * Démarre le thread de cleanup
	 * @param threadName le nom du thread du timer utilisé
	 */
	public void start(String threadName) {
		cleanupTimer = new Timer(threadName);
		cleanupTimer.schedule(buildCleanupTask(), cleanupPeriod * 1000L, cleanupPeriod * 1000L);
	}

	/**
	 * Arrête le thread de cleanup
	 */
	public void stop() throws Exception {
		cleanupTimer.cancel();
		cleanupTimer = null;
	}
}
