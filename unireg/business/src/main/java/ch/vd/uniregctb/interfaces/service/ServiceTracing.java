package ch.vd.uniregctb.interfaces.service;

import java.util.Map;

/**
 * Classe utilitaire qui permet de comptabiliser le ping moyen (depuis le début de l'application et sur les 5 dernières minutes) ainsi que
 * le temps passé entre deux appels.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public final class ServiceTracing implements ServiceTracingInterface {

	private static final long NANO_TO_MILLI = 1000000;
	private static final long UNE_MINUTE = 60000 * NANO_TO_MILLI;
	private static final int RECENTS_SIZE = 5; // 5 minutes d'activité récente

	private static class Data {
		public long time = 0;
		public int calls = 0;
	}

	/**
	 * Le timestamp (nanosecondes) du dernier appel au service
	 */
	private long lastCallTime = 0;

	/**
	 * Les données totales cumulées depuis le démarrage de l'application
	 */
	private final Data total = new Data();

	/**
	 * Les données récentes (5 dernières minutes d'activité)
	 */
	private final Data[] recents = new Data[RECENTS_SIZE];
	private int index;
	private long indexTime;

	public ServiceTracing() {
		for (int i = 0; i < recents.length; ++i) {
			recents[i] = new Data();
		}
		index = 0;
		indexTime = System.nanoTime();
	}

	public long getLastCallTime() {
		return lastCallTime;
	}

	public long getTotalTime() {
		synchronized (this) {
			return total.time;
		}
	}

	public long getTotalPing() {
		long ping = 0;
		synchronized (this) {
			if (total.calls > 0) {
				ping = (total.time / total.calls) / NANO_TO_MILLI;
			}
		}
		return ping;
	}

	public long getTotalCount() {
		synchronized (this) {
			return total.calls;
		}
	}

	public long getRecentTime() {
		synchronized (this) {
			long time = 0;
			for (Data recent : recents) {
				time += recent.time;
			}
			return time;
		}
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

	protected void addTime(long time) {
		synchronized (this) {
			total.time += time;
			total.calls++;

			long now = System.nanoTime();
			if (now > indexTime + UNE_MINUTE) {
				// si le dernier index est plus vieux qu'une minute, on décale d'un cran la liste des données récentes (comportement
				// rotatif)
				if (++index >= recents.length) {
					index = 0;
				}
				recents[index].time = 0;
				recents[index].calls = 0;
				indexTime = now;
			}

			recents[index].time += time;
			recents[index].calls++;
		}
	}

	/**
	 * Signale le début d'un appel d'une méthode. La valeur retournée doit être transmise à la méthode {@link #end(long)}.
	 */
	public long start() {
		return System.nanoTime();
	}

	/**
	 * Signale la fin d'un appel d'une méthode
	 *
	 * @param start
	 *            la valeur retournée par la méthode {@link #start()}.
	 */
	public void end(long start) {
		final long nanoTime = System.nanoTime();
		lastCallTime = nanoTime;
		addTime(nanoTime - start);
	}

	public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
		return null;
	}
}
