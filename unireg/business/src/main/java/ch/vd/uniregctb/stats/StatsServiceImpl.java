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

import net.sf.ehcache.Ehcache;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;

public class StatsServiceImpl implements InitializingBean, DisposableBean, StatsService {

	private static final Logger LOGGER = Logger.getLogger(StatsServiceImpl.class);

	private static long LOG_PERIODE = 300000L; // 5 minutes

	private final Timer timer = new Timer();
	private final Map<String, ServiceTracingInterface> rawServices = new HashMap<String, ServiceTracingInterface>();
	private final Map<String, Ehcache> cachedServices = new HashMap<String, Ehcache>();
	private final Map<String, LoadMonitor> loadMonitors = new HashMap<String, LoadMonitor>();
	private long lastLoggedCallTime = 0;

	private final TimerTask task = new TimerTask() {
		@Override
		public void run() {
			logStats();
		}
	};

	public void registerService(String serviceName, ServiceTracingInterface tracing) {
		synchronized (rawServices) {
			rawServices.put(serviceName, tracing);
		}
	}

	public void registerCache(String serviceName, Ehcache cache) {
		synchronized (cachedServices) {
			cachedServices.put(serviceName, cache);
		}
	}

	public void registerLoadMonitor(String serviceName, LoadMonitor monitor) {
		synchronized (loadMonitors) {
			loadMonitors.put(serviceName, monitor);
		}
	}

	public void unregisterService(String serviceName) {
		synchronized (rawServices) {
			rawServices.remove(serviceName);
		}
	}

	public void unregisterCache(String serviceName) {
		synchronized (cachedServices) {
			cachedServices.remove(serviceName);
		}
	}

	public void unregisterLoadMonitor(String serviceName) {
		synchronized (loadMonitors) {
			loadMonitors.remove(serviceName);
		}
	}

	private CacheStats getCacheStats(String cacheName) {

		final Ehcache cache;
		synchronized (cachedServices) {
			cache = cachedServices.get(cacheName);
		}

		if (cache == null) {
			return null;
		}

		return new CacheStats(cache);
	}

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
		synchronized (cachedServices) {
			keys.addAll(cachedServices.keySet());
		}

		// extrait et analyse les stats des services
		long lastCallTime = 0;
		for (String k : keys) {
			final ServiceStats data = getServiceStats(k);
			lastCallTime = Math.max(lastCallTime, data.getLastCallTime());
		}

		return lastCallTime;
	}

	public String buildStats() {

		StringBuilder b = new StringBuilder("Statistiques des caches et services:\n\n");
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
		List<String> sortedKeys = new ArrayList<String>(keys);
		Collections.sort(sortedKeys);

		// extrait et analyse les stats des services
		int maxLen = 0; // longueur maximale des clés
		final List<CacheStats> stats = new ArrayList<CacheStats>(sortedKeys.size());
		for (String k : sortedKeys) {
			CacheStats data = getCacheStats(k);
			stats.add(data);
			maxLen = Math.max(maxLen, k.length());
		}

		StringBuilder b = new StringBuilder();
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

		if (keys.size() == 0) {
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
		for (int i = 0 ; i < count ; ++ i) {
			final String k = sortedKeys.get(i);
			final LoadMonitorStats data = stats.get(i);
			b.append(printLine(maxLen, k, data));
		}

		return b.toString();
	}

	private String printLine(int maxLen, String key, LoadMonitorStats data) {

		final String chargeInstantannee = String.format("%d", data.getChargeInstantannee());
		final String moyenneCharge = String.format("%#13.3f", data.getMoyenneCharge());

		final StringBuilder b = new StringBuilder();
		b.append(' ').append(rpad(key, maxLen)).append(" | ");
		b.append(lpad(chargeInstantannee, 7)).append(" | ");
		b.append(moyenneCharge);
		b.append('\n');

		return b.toString();
	}

	private String printLine(int maxLen, final String key, final CacheStats data) {

		final StringBuilder b = new StringBuilder();

		final String hitPercent = (data.getHitsPercent() == null ? "-" : String.format("%d%%", data.getHitsPercent()));
		final String hitCount = (data.getHitsCount() == null ? "-" : String.format("%9d", data.getHitsCount()));
		final String totalCount = (data.getTotalCount() == null ? "-" : String.format("%9d", data.getTotalCount()));

		b.append(' ').append(rpad(key, maxLen)).append(" | ");
		b.append(lpad(hitPercent, 12)).append(" | ");
		b.append(lpad(hitCount, 10)).append(" | ");
		b.append(lpad(totalCount, 11)).append(" | ");
		b.append(lpad(String.valueOf(data.getTimeToIdle()), 12)).append(" | ");
		b.append(lpad(String.valueOf(data.getTimeToLive()), 12)).append(" | ");
		b.append(lpad(String.valueOf(data.getMaxElements()), 11));
		b.append('\n');

		return b.toString();
	}

	private String printLine(int maxLen, final String key, final ServiceStats data) {

		final StringBuilder b = new StringBuilder();

		final String recentPing = (data.getRecentPing() == null ? "-   " : String.format("%d ms", data.getRecentPing()));
		final String recentCount = (data.getRecentCount() == null ? "-" : String.format("%9d", data.getRecentCount()));
		final String totalPing = (data.getTotalPing() == null ? "-   " : String.format("%d ms", data.getTotalPing()));
		final String totalCount = (data.getTotalCount() == null ? "-" : String.format("%9d", data.getTotalCount()));

		b.append(' ').append(rpad(key, maxLen)).append(" | ");
		b.append(lpad(recentPing, 10)).append(" | ");
		b.append(lpad(recentCount, 10)).append(" | ");
		b.append(lpad(totalPing, 10)).append(" | ");
		b.append(lpad(totalCount, 10));
		b.append('\n');

		return b.toString();
	}


	/**
	 * Complète la chaîne de caractères spécifiée avec des espaces au début de manière à ce qu'elle atteigne le longueur spécifiée.
	 *
	 * @param s
	 *            la chaîne de caractères à padder
	 * @param len
	 *            la longueur désirée
	 * @return un chaîne de caractères de longueur minimale <i>len</i>.
	 */
	private static String lpad(String s, int len) {
		final int l = s.length();
		if (l >= len) {
			return s;
		}
		return StringUtils.repeat(" ", len - l) + s;
	}

	/**
	 * Complète la chaîne de caractères spécifiée avec des espaces à la fin de manière à ce qu'elle atteigne le longueur spécifiée.
	 *
	 * @param s
	 *            la chaîne de caractères à padder
	 * @param len
	 *            la longueur désirée
	 * @return un chaîne de caractères de longueur minimale <i>len</i>.
	 */
	@SuppressWarnings("unused")
	private static String rpad(String s, int len) {
		final int l = s.length();
		if (l >= len) {
			return s;
		}
		return s + StringUtils.repeat(" ", len - l);
	}

	public void afterPropertiesSet() throws Exception {
		if (LOGGER.isInfoEnabled()) {
			timer.schedule(task, LOG_PERIODE, LOG_PERIODE);
		}
	}

	public void destroy() throws Exception {
		timer.cancel();
	}
}
