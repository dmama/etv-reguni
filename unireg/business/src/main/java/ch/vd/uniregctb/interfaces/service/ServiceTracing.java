package ch.vd.uniregctb.interfaces.service;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;

/**
 * Classe utilitaire qui permet de comptabiliser le ping moyen (depuis le début de l'application et sur les 5 dernières minutes) ainsi que le temps passé entre deux appels.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public final class ServiceTracing implements ServiceTracingInterface {

	private static final long NANO_TO_MILLI = 1000000;
	private static final int RECENTS_SIZE = 5; // 5 minutes d'activité récente

	/**
	 * Statistiques d'appel pour un service ou une méthode
	 */
	private class Data implements ServiceTracingInterface {

		/**
		 * Le temps (en nanosecondes) passé dans le service ou la méthode
		 */
		public long time = 0;
		/**
		 * Le nombre d'appels du service ou de la méthode.
		 */
		public int calls = 0;
		/**
		 * Le nombre d'éléments considérés pour les appels faits au service ou à la méthode.
		 */
		public long items = 0;

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
			this.items = right.items;

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

		@Override
		public long getLastCallTime() {
			return lastCallTime;
		}

		@Override
		public long getTotalTime() {
			return time;
		}

		@Override
		public long getTotalCount() {
			return calls;
		}

		@Override
		public long getTotalPing() {
			long ping = 0;
			synchronized (this) {
				if (calls > 0) {
					ping = (time / calls) / NANO_TO_MILLI;
				}
			}
			return ping;
		}

		@Override
		public long getTotalItemsCount() {
			return items;
		}

		@Override
		public long getTotalItemsPing() {
			long ping = 0;
			synchronized (this) {
				if (items > 0) {
					ping = (time / items) / NANO_TO_MILLI;
				}
			}
			return ping;
		}

		@Override
		public long getRecentTime() {
			long time = 0;
			synchronized (this) {
				for (Data recent : recents) {
					time += recent.time;
				}
			}
			return time;
		}

		@Override
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

		@Override
		public long getRecentCount() {
			long calls = 0;

			synchronized (this) {
				for (Data recent : recents) {
					calls += recent.calls;
				}
			}
			
			return calls;
		}

		@Override
		public long getRecentItemsCount() {
			long items = 0;

			synchronized (this) {
				for (Data recent : recents) {
					items += recent.items;
				}
			}

			return items;
		}

		@Override
		public long getRecentItemsPing() {
			long items = 0;
			long time = 0;

			synchronized (this) {
				for (Data recent : recents) {
					time += recent.time;
					items += recent.items;
				}
			}

			long ping = 0;
			if (items > 0) {
				ping = (time / items) / NANO_TO_MILLI;
			}
			return ping;
		}

		@Override
		public Map<String, ? extends ServiceTracingInterface> getDetailedData() {
			return null;
		}

		@Override
		public void onTick() {
			throw new NotImplementedException();
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

	public ServiceTracing(String serviceName) {
		this.index = 0;
		this.detailLogger = Logger.getLogger(String.format("%s.%s", ServiceTracing.class.getSimpleName(), serviceName));
	}

	@Override
	public long getLastCallTime() {
		return lastCallTime;
	}

	@Override
	public long getTotalTime() {
		return total.getTotalTime();
	}

	@Override
	public long getTotalPing() {
		return total.getTotalPing();
	}

	@Override
	public long getTotalCount() {
		return total.getTotalCount();
	}

	@Override
	public long getTotalItemsCount() {
		return total.getTotalItemsCount();
	}

	@Override
	public long getTotalItemsPing() {
		return total.getTotalItemsPing();
	}

	@Override
	public long getRecentTime() {
		return total.getRecentTime();
	}

	@Override
	public long getRecentPing() {
		return total.getRecentPing();
	}

	@Override
	public long getRecentCount() {
		return total.getRecentCount();
	}

	@Override
	public long getRecentItemsCount() {
		return total.getRecentItemsCount();
	}

	@Override
	public long getRecentItemsPing() {
		return total.getRecentItemsPing();
	}

	@Override
	public void onTick() {
		synchronized (total) {
			shiftRecent();
		}
	}

	protected void addTime(long time) {
		synchronized (total) {
			total.time += time;
			total.calls++;
			total.items++;

			total.recents[index].time += time;
			total.recents[index].calls++;
			total.recents[index].items++;
		}
	}

	protected void addTime(long time, String name) {
		addTime(time, 1, name);
	}

	protected void addTime(long time, int items, String name) {
		synchronized (total) {
			total.time += time;
			total.calls++;
			total.items += items;

			Data d = details.get(name);
			if (d == null) {
				d = new Data(true);
				details.put(name, d);
			}

			d.time += time;
			d.calls++;
			d.items += items;

			total.recents[index].time += time;
			total.recents[index].calls++;
			total.recents[index].items += items;

			d.recents[index].time += time;
			d.recents[index].calls++;
			d.recents[index].items += items;
		}
	}

	private void shiftRecent() {

		if (++index >= total.recents.length) {
			index = 0;
		}

		// on remet à zéro les compteurs
		total.recents[index].time = 0;
		total.recents[index].calls = 0;
		total.recents[index].items = 0;

		for (Data d : details.values()) {
			d.recents[index].time = 0;
			d.recents[index].calls = 0;
			d.recents[index].items = 0;
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
	 * Signale la fin d'un appel d'une méthode nommée (le temps de réponse est loggué en niveau {@link org.apache.log4j.Level#INFO INFO})
	 *
	 * @param start la valeur retournée par la méthode {@link #start()}.
	 * @param name  le nom de la méthode
	 * @param params un objet dont l'appel à la méthode {@link Object#toString() toString()} sera utilisé pour décrire les paramètres de la méthode
	 */
	public void end(long start, String name, @Nullable Object params) {
		end(start, null, name, params);
	}

	/**
	 * Signale la fin d'un appel d'une méthode nommée (le temps de réponse est loggué en niveau {@link org.apache.log4j.Level#INFO INFO}),
	 * en ajoutant, le cas échéant, la classe de l'exception levée par l'appel
	 *
	 * @param start la valeur retournée par la méthode {@link #start()}.
	 * @param thrown l'exception éventuellement reçue dans l'appel
	 * @param name  le nom de la méthode
	 * @param params un objet dont l'appel à la méthode {@link Object#toString() toString()} sera utilisé pour décrire les paramètres de la méthode
	 */
	public void end(long start, @Nullable Throwable thrown, String name, @Nullable Object params) {
		final long nanoTime = System.nanoTime();
		lastCallTime = nanoTime;
		addTime(nanoTime - start, name);
		if (detailLogger.isInfoEnabled()) {
			final String paramString;
			if (params != null) {
				paramString = params.toString();
			}
			else {
				paramString = null;
			}
			final String throwableString;
			if (thrown != null) {
				throwableString = String.format(", %s thrown", thrown.getClass().getName());
			}
			else {
				throwableString = StringUtils.EMPTY;
			}
			if (StringUtils.isBlank(paramString)) {
				detailLogger.info(String.format("(%d ms) %s%s", (nanoTime - start) / 1000000, name, throwableString));
			}
			else {
				detailLogger.info(String.format("(%d ms) %s{%s}%s", (nanoTime - start) / 1000000, name, paramString, throwableString));
			}
		}
	}

	/**
	 * Signale la fin d'un appel d'une méthode nommée (le temps de réponse est loggué en niveau {@link org.apache.log4j.Level#INFO INFO}),
	 * en ajoutant, le cas échéant, la classe de l'exception levée par l'appel
	 *
	 * @param start la valeur retournée par la méthode {@link #start()}.
	 * @param thrown l'exception éventuellement reçue dans l'appel
	 * @param name  le nom de la méthode
	 * @param items le nombre d'éléments à prendre en compte dans l'appel de la méthode (sera utilisé pour calculer une moyenne du temps de réponse par élément).
	 * @param params un objet dont l'appel à la méthode {@link Object#toString() toString()} sera utilisé pour décrire les paramètres de la méthode
	 */
	public void end(long start, @Nullable Throwable thrown, String name, int items, @Nullable Object params) {
		final long nanoTime = System.nanoTime();
		lastCallTime = nanoTime;
		addTime(nanoTime - start, items, name);
		if (detailLogger.isInfoEnabled()) {
			final String paramString;
			if (params != null) {
				paramString = params.toString();
			}
			else {
				paramString = null;
			}
			final String throwableString;
			if (thrown != null) {
				throwableString = String.format(", %s thrown", thrown.getClass().getName());
			}
			else {
				throwableString = StringUtils.EMPTY;
			}
			if (StringUtils.isBlank(paramString)) {
				detailLogger.info(String.format("(%d ms) %s => %d items%s", (nanoTime - start) / 1000000, name, items, throwableString));
			}
			else {
				detailLogger.info(String.format("(%d ms) %s{%s} => %d items%s", (nanoTime - start) / 1000000, name, paramString, items, throwableString));
			}
		}
	}

	/**
	 * Les temps de réponses (voir méthode {@link #end(long, String, Object) end()}) sont loggués en niveau {@link org.apache.log4j.Level#INFO INFO} ;
	 * cette méthode permet d'inclure un peu plus de détails seulement en mode debug
	 * @return <code>true</code> si le logguer est actif au niveau {@link org.apache.log4j.Level#DEBUG DEBUG}, <code>false</code> sinon
	 */
	public boolean isDebugEnabled() {
		return detailLogger.isDebugEnabled();
	}

	@Override
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

	public static String toString(RegDate date) {
		if (date != null) {
			return RegDateHelper.dateToDisplayString(date);
		}
		else {
			return "null";
		}
	}

	public static <T> String toString(T[] array) {
		return Arrays.toString(array);
	}

	public static <T> String toString(Collection<T> col) {
		final Object[] array;
		if (col != null) {
			array = col.toArray(new Object[col.size()]);
		}
		else {
			array = null;
		}
		return toString(array);
	}
}
