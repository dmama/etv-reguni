package ch.vd.uniregctb.stats;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
import ch.vd.uniregctb.common.StringHelper;
import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;

public class StatsServiceImpl implements InitializingBean, DisposableBean, StatsService {

	private static final Logger LOGGER = Logger.getLogger(StatsServiceImpl.class);

	private static final long UNE_MINUTE = 60000L;
	private static final int LOG_PERIODE = 5; // 5 * UNE_MINUTE

	private final Timer timer = new Timer("StatsServiceTicking");
	private final Map<String, ServiceTracingInterface> rawServices = new HashMap<String, ServiceTracingInterface>();
	private final Map<String, UniregCacheInterface> cachedServices = new HashMap<String, UniregCacheInterface>();
	private final Map<String, LoadMonitor> loadMonitors = new HashMap<String, LoadMonitor>();
	private long lastLoggedCallTime = 0;

	private final class TickingTask extends TimerTask {

		/**
		 * Compteur d'appels à la méthode {@link #run} : on loggue en plus les statistiques tous les {@link #LOG_PERIODE} appels
		 */
		private int compteur = 0;

		@Override
		public void run() {

			// on loggue d'abord les stats avant de faire glisser
			compteur = (compteur + 1) % LOG_PERIODE;
			if (compteur == 0 && LOGGER.isInfoEnabled()) {
				logStats();
			}

			// la minute est finie -> "top"!
			synchronized (rawServices) {
				for (Map.Entry<String, ServiceTracingInterface> entry : rawServices.entrySet()) {
					try {
						entry.getValue().onTick();
					}
					catch (Exception e) {
						LOGGER.warn(String.format("Le service %s a renvoyé une exception lors du 'top' de la minute", entry.getKey()), e);
					}
				}
			}
		}
	}

	@Override
	public void registerService(String serviceName, ServiceTracingInterface tracing) {
		synchronized (rawServices) {
			rawServices.put(serviceName, tracing);
		}
	}

	@Override
	public void registerCache(String serviceName, UniregCacheInterface cache) {
		synchronized (cachedServices) {
			cachedServices.put(serviceName, cache);
		}
	}

	@Override
	public void registerLoadMonitor(String serviceName, LoadMonitor monitor) {
		synchronized (loadMonitors) {
			loadMonitors.put(serviceName, monitor);
		}
	}

	@Override
	public void unregisterService(String serviceName) {
		synchronized (rawServices) {
			rawServices.remove(serviceName);
		}
	}

	@Override
	public void unregisterCache(String serviceName) {
		synchronized (cachedServices) {
			cachedServices.remove(serviceName);
		}
	}

	@Override
	public void unregisterLoadMonitor(String serviceName) {
		synchronized (loadMonitors) {
			loadMonitors.remove(serviceName);
		}
	}

	private CacheStats getCacheStats(String cacheName) {

		final UniregCacheInterface cache;
		synchronized (cachedServices) {
			cache = cachedServices.get(cacheName);
		}

		if (cache == null) {
			return null;
		}

		return cache.buildStats();
	}

	@Override
	public ServiceStats getServiceStats(String serviceName) {

		final ServiceTracingInterface rawService;
		synchronized (rawServices) {
			rawService = rawServices.get(serviceName);
		}

		if (rawService == null) {
			return null;
		}

		return new ServiceStats(rawService);
	}

	private LoadMonitorStats getLoadMonitorStats(String serviceName) {
		final LoadMonitor service;
		synchronized (loadMonitors) {
			service = loadMonitors.get(serviceName);
		}

		if (service == null) {
			return null;
		}

		return new LoadMonitorStats(service);
	}

	private static String subKey(String key) {
		return " - " + key;
	}

	private void logStats() {

		int count;
		synchronized (rawServices) {
			count = rawServices.size();
		}
		synchronized (cachedServices) {
			count += cachedServices.size();
		}
		if (count == 0) {
			// rien à faire
			return;
		}

		final long lastCallTime = getLastCallTime();
		if (lastCallTime <= lastLoggedCallTime) {
			// s'il n'y a eu aucun appel depuis la dernière fois, on ne log rien
			return;
		}
		lastLoggedCallTime = lastCallTime;

		final String info = buildStats();
		LOGGER.info(info);
	}

	private long getLastCallTime() {

		// on récupère les noms des services
		final Set<String> keys = new HashSet<String>();
		synchronized (rawServices) {
			keys.addAll(rawServices.keySet());
		}

		// extrait et analyse les stats des services
		long lastCallTime = 0;
		for (String k : keys) {
			final ServiceStats data = getServiceStats(k);
			if (data != null) {
				lastCallTime = Math.max(lastCallTime, data.getLastCallTime());
			}
		}

		return lastCallTime;
	}

	@Override
	public String buildStats() {

		final StringBuilder b = new StringBuilder("Statistiques des caches et services:\n\n");
		b.append(buildCacheStats());
		b.append('\n');
		b.append(buildServiceStats());
		b.append('\n');
		b.append(buildLoadMonitorStats());

		return b.toString();
	}

	private String buildCacheStats() {

		// on récupère les noms des caches
		final Set<String> keys = new HashSet<String>();
		synchronized (cachedServices) {
			keys.addAll(cachedServices.keySet());
		}

		// on trie les clés avant de les afficher
		final List<String> sortedKeys = new ArrayList<String>(keys);
		Collections.sort(sortedKeys);

		// extrait et analyse les stats des services
		int maxLen = 0; // longueur maximale des clés
		final List<CacheStats> stats = new ArrayList<CacheStats>(sortedKeys.size());
		for (String k : sortedKeys) {
			final CacheStats data = getCacheStats(k);
			stats.add(data);
			maxLen = Math.max(maxLen, k.length());
		}

		final StringBuilder b = new StringBuilder();
		b.append(" Caches").append(StringUtils.repeat(" ", maxLen - 6));
		b.append(" | hits percent | hits count | total count | time-to-idle | time-to-live | max elements\n");
		b.append(StringUtils.repeat("-", maxLen + 1));
		b.append("-+--------------+------------+-------------+--------------+--------------+--------------\n");

		final int count = stats.size();
		for (int i = 0; i < count; ++i) {
			final String k = sortedKeys.get(i);
			final CacheStats data = stats.get(i);
			b.append(printLine(maxLen, k, data));
		}

		return b.toString();
	}

	public String buildServiceStats() {

		// on récupère les noms des services
		final Set<String> keys = new HashSet<String>();
		synchronized (rawServices) {
			keys.addAll(rawServices.keySet());
		}


		// on trie les clés avant de les afficher
		List<String> sortedKeys = new ArrayList<String>(keys);
		Collections.sort(sortedKeys);

		// extrait et analyse les stats des services
		int maxLen = 0; // longueur maximale des clés
		final List<ServiceStats> stats = new ArrayList<ServiceStats>(sortedKeys.size());
		for (String k : sortedKeys) {
			final ServiceStats data = getServiceStats(k);
			stats.add(data);
			maxLen = Math.max(maxLen, k.length());

			final Map<String, ServiceStats> subData = data.getDetailedData();
			for (Map.Entry<String, ServiceStats> e : subData.entrySet()) {
				maxLen = Math.max(maxLen, subKey(e.getKey()).length());
			}
		}

		StringBuilder b = new StringBuilder();
		b.append(StringUtils.repeat(" ", maxLen + 1));
		b.append(" |     (last 5 minutes)    |      (since start)\n");
		b.append(" Services").append(StringUtils.repeat(" ", maxLen - 8));
		b.append(" |    ping    | hits count |    ping    | hits count\n");
		b.append(StringUtils.repeat("-", maxLen + 1));
		b.append("-+------------+------------+------------+------------\n");

		final int count = stats.size();
		for (int i = 0; i < count; ++i) {
			final String k = sortedKeys.get(i);
			final ServiceStats data = stats.get(i);
			b.append(printLine(maxLen, k, data));

			final Map<String, ServiceStats> subData = data.getDetailedData();
			for (Map.Entry<String, ServiceStats> e : subData.entrySet()) {
				b.append(printLine(maxLen, subKey(e.getKey()), e.getValue()));
			}
		}

		return b.toString();
	}

	private String buildLoadMonitorStats() {

		// on récupère les noms des caches
		final Set<String> keys = new HashSet<String>();
		synchronized (loadMonitors) {
			keys.addAll(loadMonitors.keySet());
		}

		if (keys.isEmpty()) {
			return StringUtils.EMPTY;
		}

		// on trie les clés avant de les afficher
		final List<String> sortedKeys = new ArrayList<String>(keys);
		Collections.sort(sortedKeys);

		// extrait et analyse les stats des services
		int maxLen = 0; // longueur maximale des clés
		final List<LoadMonitorStats> stats = new ArrayList<LoadMonitorStats>(sortedKeys.size());
		for (String k : sortedKeys) {
			final LoadMonitorStats data = getLoadMonitorStats(k);
			stats.add(data);
			maxLen = Math.max(maxLen, k.length());
		}

		final StringBuilder b = new StringBuilder();
		b.append(" Load").append(StringUtils.repeat(" ", maxLen - 4));
		b.append(" | current | 5-min average\n");
		b.append(StringUtils.repeat("-", maxLen + 1));
		b.append("-+---------+---------------\n");

		final int count = stats.size();
		for (int i = 0; i < count; ++i) {
			final String k = sortedKeys.get(i);
			final LoadMonitorStats data = stats.get(i);
			b.append(printLine(maxLen, k, data));
		}

		return b.toString();
	}

	private String printLine(int maxLen, String key, LoadMonitorStats data) {

		final String chargeInstantanee = String.format("%d", data.getChargeInstantanee());
		final String moyenneCharge = String.format("%#13.3f", data.getMoyenneCharge());

		final StringBuilder b = new StringBuilder();
		b.append(' ').append(StringHelper.rpad(key, maxLen)).append(" | ");
		b.append(StringHelper.lpad(chargeInstantanee, 7)).append(" | ");
		b.append(moyenneCharge);
		b.append('\n');

		return b.toString();
	}

	private String printLine(int maxLen, final String key, final CacheStats data) {

		final StringBuilder b = new StringBuilder();

		final String hitPercent = (data.getHitsPercent() == null ? "-" : String.format("%d%%", data.getHitsPercent()));
		final String hitCount = String.format("%9d", data.getHitsCount());
		final String totalCount = String.format("%9d", data.getTotalCount());
		final String timeToIdle = (data.getTimeToIdle() == null ? "-" : String.valueOf(data.getTimeToIdle()));
		final String timeToLive = (data.getTimeToLive() == null ? "-" : String.valueOf(data.getTimeToLive()));
		final String maxElements = (data.getMaxElements() == null ? "-" : String.valueOf(data.getMaxElements()));

		b.append(' ').append(StringHelper.rpad(key, maxLen)).append(" | ");
		b.append(StringHelper.lpad(hitPercent, 12)).append(" | ");
		b.append(StringHelper.lpad(hitCount, 10)).append(" | ");
		b.append(StringHelper.lpad(totalCount, 11)).append(" | ");
		b.append(StringHelper.lpad(timeToIdle, 12)).append(" | ");
		b.append(StringHelper.lpad(timeToLive, 12)).append(" | ");
		b.append(StringHelper.lpad(maxElements, 11));
		b.append('\n');

		return b.toString();
	}

	private String printLine(int maxLen, final String key, final ServiceStats data) {

		final StringBuilder b = new StringBuilder();

		final String recentPing = (data.getRecentPing() == null ? "-   " : String.format("%d ms", data.getRecentPing()));
		final String recentCount = (data.getRecentCount() == null ? "-" : String.format("%9d", data.getRecentCount()));
		final String totalPing = (data.getTotalPing() == null ? "-   " : String.format("%d ms", data.getTotalPing()));
		final String totalCount = (data.getTotalCount() == null ? "-" : String.format("%9d", data.getTotalCount()));

		b.append(' ').append(StringHelper.rpad(key, maxLen)).append(" | ");
		b.append(StringHelper.lpad(recentPing, 10)).append(" | ");
		b.append(StringHelper.lpad(recentCount, 10)).append(" | ");
		b.append(StringHelper.lpad(totalPing, 10)).append(" | ");
		b.append(StringHelper.lpad(totalCount, 10));
		b.append('\n');

		return b.toString();
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		timer.schedule(new TickingTask(), UNE_MINUTE, UNE_MINUTE);
	}

	@Override
	public void destroy() throws Exception {
		timer.cancel();
	}
}
