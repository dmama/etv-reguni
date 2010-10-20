package ch.vd.uniregctb.interfaces.service;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * Classe utilitaire qui permet de comptabiliser le ping moyen (depuis le début de l'application et sur les 5 dernières minutes) ainsi que le temps passé entre deux appels.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public final class ServiceTracing implements ServiceTracingInterface {

	private static final long NANO_TO_MILLI = 1000000;
	private static final long UNE_MINUTE = 60000 * NANO_TO_MILLI;
	private static final int RECENTS_SIZE = 5; // 5 minutes d'activité récente

	private class Data implements ServiceTracingInterface {

		public long time = 0;
		public int calls = 0;

		/**
		 * Les données récentes (5 dernières minutes d'activité)
		 */
		private final Data[] recents;

		private Data(boolean withRecents) {
			if (withRecents) {
				this.recents = new Data[RECENTS_SIZE];
				for (int i = 0; i < RECENTS_SIZE; ++i) {
					this.recents[i] = new Data(false);
				}
			}
			else {
				this.recents = null;
			}
		}

		private Data(Data right) {
			this.time = right.time;
			this.calls = right.calls;

			if (right.recents != null) {
				this.recents = new Data[right.recents.length];
				for (int i = 0; i < right.recents.length; i++) {
					this.recents[i] = new Data(right.recents[i]);
				}
			}
			else {
				this.recents = null;
			}
		}

		public long getLastCallTime() {
			return lastCallTime;
		}

		public long getTotalTime() {
			return time;
		}

		public long getTotalCount() {
			return calls;
		}

		public long getTotalPing() {
			long ping = 0;
			synchronized (this) {
				if (calls > 0) {
					ping = (time / calls) / NANO_TO_MILLI;
				}
			}
			return ping;
		}

		public long getRecentTime() {
			long time = 0;
			synchronized (this) {
				for (Data recent : recents) {
					time += recent.time;
				}
			}
			return time;
		}

		public long getRecentPing() {
			long calls = 0;
			long time = 0;

			synchronized (this) {
				for (Data recent : recents) {
					time += recent.time;
					calls += recent.calls;
				}
			}

			long ping = 0;
			if (calls > 0) {
				ping = (time / calls) / NANO_TO_MILLI;
			}
			return ping;
		}

		public long getRecentCount() {
			long calls = 0;

			synchronized (this) {
				for (Data recent : recents) {
					calls += recent.calls;
				}
			}
			
			return calls;
		}

		public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
			return null;
		}
	}

	/**
	 * Le timestamp (nanosecondes) du dernier appel au service
	 */
	private long lastCallTime = 0;

	/**
	 * Les données totales cumulées depuis le démarrage de l'application
	 */
	private final Data total = new Data(true);

	/**
	 * Les données détaillées et cumulées depuis le démarrage de l'application
	 */
	private final Map<String, Data> details = new HashMap<String, Data>();

	/**
	 * Le logger utilisé dans les traces détaillées
	 */
	private final Logger detailLogger;

	private int index;
	private long indexTime;

	public ServiceTracing(String serviceName) {
		this.index = 0;
		this.indexTime = System.nanoTime();
		this.detailLogger = Logger.getLogger(String.format("%s.%s", ServiceTracing.class.getSimpleName(), serviceName));
	}

	public long getLastCallTime() {
		return lastCallTime;
	}

	public long getTotalTime() {
		return total.getTotalTime();
	}

	public long getTotalPing() {
		return total.getTotalPing();
	}

	public long getTotalCount() {
		return total.getTotalCount();
	}

	public long getRecentTime() {
		return total.getRecentTime();
	}

	public long getRecentPing() {
		return total.getRecentPing();
	}

	public long getRecentCount() {
		return total.getRecentCount();
	}

	protected void addTime(long time) {
		synchronized (total) {
			total.time += time;
			total.calls++;

			shiftRecent();

			total.recents[index].time += time;
			total.recents[index].calls++;
		}
	}

	protected void addTime(long time, String name) {
		synchronized (total) {
			total.time += time;
			total.calls++;

			Data d = details.get(name);
			if (d == null) {
				d = new Data(true);
				details.put(name, d);
			}

			d.time += time;
			d.calls++;

			shiftRecent();

			total.recents[index].time += time;
			total.recents[index].calls++;

			d.recents[index].time += time;
			d.recents[index].calls++;
		}
	}

	private void shiftRecent() {
		long now = System.nanoTime();
		if (now > indexTime + UNE_MINUTE) {
			// si le dernier index est plus vieux qu'une minute, on décale d'un cran la liste des données récentes (comportement rotatif)
			if (++index >= total.recents.length) {
				index = 0;
			}

			// on remet à zéro les compteurs
			total.recents[index].time = 0;
			total.recents[index].calls = 0;

			for (Data d : details.values()) {
				d.recents[index].time = 0;
				d.recents[index].calls = 0;
			}

			indexTime = now;
		}
	}

	/**
	 * Signale le début d'un appel d'une méthode. La valeur retournée doit être transmise à la méthode {@link #end(long)}.
	 *
	 * @return un timestamp à transmettre à la méthode end().
	 */
	public long start() {
		return System.nanoTime();
	}

	/**
	 * Signale la fin d'un appel d'une méthode
	 *
	 * @param start la valeur retournée par la méthode {@link #start()}.
	 */
	public void end(long start) {
		final long nanoTime = System.nanoTime();
		lastCallTime = nanoTime;
		addTime(nanoTime - start);
	}

	/**
	 * Signale la fin d'un appel d'une méthode nommée
	 *
	 * @param start la valeur retournée par la méthode {@link #start()}.
	 * @param name  le nom de la méthode
	 */
	public void end(long start, String name) {
		final long nanoTime = System.nanoTime();
		lastCallTime = nanoTime;
		addTime(nanoTime - start, name);
		if (detailLogger.isInfoEnabled()) {
			detailLogger.info(String.format("(%d ms) %s", (nanoTime - start) / 1000000, name));
		}
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		Map<String, Data> copy;
		// fait une copie complète des données pour éviter des problèmes d'accès concurrents
		synchronized (total) {
			copy = new HashMap<String, Data>(details.size());
			for (Map.Entry<String, Data> e: details.entrySet()) {
				copy.put(e.getKey(), new Data(e.getValue()));
			}
		}
		return copy;
	}
}
