package ch.vd.uniregctb.webservices.common;

import java.util.Map;

import org.apache.cxf.management.counters.ResponseTimeCounter;

import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;

/**
 * Permet d'adapter un ResponseTimeCounter à l'interface ServiceTracingInterface.
 */
public class ResponseTimeTracingCounter implements ServiceTracingInterface {

	private static final int LOG_PERIODE = 5;

	private final ResponseTimeCounter counter;

	private final Data data = new Data();

	private class Data {

		private final long nbAppels[] = new long[LOG_PERIODE];
		private final long tpsTotal[] = new long[LOG_PERIODE];

		/**
		 * Représente l'index de la dernière valeur stockée
		 * (= celle à partir de laquelle on peut connaître l'évolution depuis le dernier "tick")
		 */
		private int index = 0;

		/**
		 * Représente la valeur la plus haute que l'index ait atteinte (utile dans les quelques
		 * premières minutes d'exécution de l'application, i.e. les premiers "ticks")
		 */
		private int indexHighWaterMark = 0;

		/**
		 * Appelé régulièrement dans le temps (= toutes les minutes, voir {@link ch.vd.uniregctb.stats.StatsServiceImpl#UNE_MINUTE}),
		 * <b>après</b> que les statistiques aient été logguées
		 */
		public synchronized void onTick() {
			++ index;
			if (index > indexHighWaterMark) {
				indexHighWaterMark = index;
			}
			index %= LOG_PERIODE;

			nbAppels[index] = getTotalCount();
			tpsTotal[index] = getTotalTime();
		}

		public synchronized long getRecentPing() {
			final long count = getRecentCount();
			if (count == 0) {
				return 0;
			}
			else {
				final long time = getRecentTime();
				return time / count;
			}
		}

		/**
		 * @return l'index qui correspond au plus vieux pointage que l'on ait fait
		 */
		private int getOldestKnownIndex() {
			final int cruisingSpeedIndex = (index + 1) % LOG_PERIODE;
			if (cruisingSpeedIndex > indexHighWaterMark) {
				return 0;
			}
			else {
				return cruisingSpeedIndex;
			}
		}

		public synchronized long getRecentTime() {
			final int oldestIndex = getOldestKnownIndex();
			return getTotalTime() - tpsTotal[oldestIndex];
		}

		public synchronized long getRecentCount() {
			final int oldestIndex = getOldestKnownIndex();
			return getTotalCount() - nbAppels[oldestIndex];
		}

		public synchronized long getLastCallTime() {
			final int oldestKnownIndex = getOldestKnownIndex();

			// s'il y a eu une modification du nombre d'appels depuis le plus vieux pointage que j'ai, alors
			// il faudra logger les stats, donc on fait comme si le dernier appel, c'était maintenant
			if (nbAppels[oldestKnownIndex] != getTotalCount()) {
				return System.nanoTime();
			}
			else {
				// sinon, cela ne doit pas déclencher d'affichage de stats 
				return 0;
			}
		}
	}

	public ResponseTimeTracingCounter(ResponseTimeCounter counter) {
		this.counter = counter;
	}

	@Override
	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
	}

	@Override
	public long getLastCallTime() {
		return data.getLastCallTime();
	}

	@Override
	public long getRecentPing() {
		return data.getRecentPing();
	}

	@Override
	public long getRecentTime() {
		return data.getRecentTime();
	}

	@Override
	public long getRecentCount() {
		return data.getRecentCount();
	}

	@Override
	public long getRecentItemsCount() {
		return data.getRecentCount();
	}

	@Override
	public long getRecentItemsPing() {
		return data.getRecentPing();
	}

	@Override
	public void onTick() {
		data.onTick();
	}

	@Override
	public long getTotalPing() {
		final long value = getTotalPingMicroseconds();
		return value / 1000L;
	}

	private long getTotalPingMicroseconds() {
		if (counter == null) {
			return 0;
		}
		return counter.getAvgResponseTime().longValue();
	}

	@Override
	public long getTotalTime() {
		if (counter == null) {
			return 0;
		}

		final long numInvocations = counter.getNumInvocations().longValue();
		final long totalPingMicroseconds = getTotalPingMicroseconds();
		return numInvocations * totalPingMicroseconds / 1000L;
	}

	@Override
	public long getTotalCount() {
		if (counter == null) {
			return 0;
		}

		return counter.getNumInvocations().longValue();
	}

	@Override
	public long getTotalItemsCount() {
		return getTotalCount();
	}

	@Override
	public long getTotalItemsPing() {
		return getTotalPing();
	}
}
