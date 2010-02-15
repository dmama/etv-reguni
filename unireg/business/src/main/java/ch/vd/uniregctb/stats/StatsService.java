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
import net.sf.ehcache.Statistics;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.interfaces.service.ServiceTracingInterface;

/**
 * Ce service centralise les informations statistiques des services de l'application.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class StatsService implements InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(StatsService.class);

	private static long LOG_PERIODE = 300000L; // 5 minutes

	private static StatsService singleton;

	private final Timer timer = new Timer();
	private final Map<String, ServiceTracingInterface> rawServices = new HashMap<String, ServiceTracingInterface>();
	private final Map<String, Ehcache> cachedServices = new HashMap<String, Ehcache>();
	private long lastLoggedCallTime = 0;

	private final TimerTask task = new TimerTask() {
		@Override
		public void run() {
			logStats();
		}
	};

	public static void registerRawService(String serviceName, ServiceTracingInterface tracing) {
		if (singleton != null) {
			singleton.registerRaw(serviceName, tracing);
		}
	}

	public static void registerCachedService(String serviceName, Ehcache cache) {
		if (singleton != null) {
			singleton.registerCached(serviceName, cache);
		}
	}

	public static void unregisterRawService(String serviceName) {
		if (singleton != null) {
			singleton.unregisterRaw(serviceName);
		}
	}

	public static void unregisterCachedService(String serviceName) {
		if (singleton != null) {
			singleton.unregisterCached(serviceName);
		}
	}

	public static StatsData getInfo(String serviceName) {
		if (singleton == null) {
			return null;
		}
		return singleton.get(serviceName);
	}

	private void registerRaw(String serviceName, ServiceTracingInterface tracing) {
		synchronized (rawServices) {
			rawServices.put(serviceName, tracing);
		}
	}

	private void registerCached(String serviceName, Ehcache cache) {
		synchronized (cachedServices) {
			cachedServices.put(serviceName, cache);
		}
	}

	private void unregisterRaw(String serviceName) {
		synchronized (rawServices) {
			rawServices.remove(serviceName);
		}
	}

	private void unregisterCached(String serviceName) {
		synchronized (cachedServices) {
			cachedServices.remove(serviceName);
		}
	}

	private StatsData get(String serviceName) {

		final ServiceTracingInterface rawService;
		synchronized (rawServices) {
			rawService = rawServices.get(serviceName);
		}

		final Ehcache cache;
		synchronized (cachedServices) {
			cache = cachedServices.get(serviceName);
		}

		if (cache == null && rawService == null) {
			return null;
		}

		final StatsData data;

		if (rawService == null) {
			data = new StatsData();
		}
		else {
			data = new StatsData(rawService);
		}

		if (cache != null) {
			final Statistics statistics = cache.getStatistics();

			final long hits = statistics.getCacheHits();
			final long misses = statistics.getCacheMisses();
			data.setHitsCount(hits);

			final long total = hits + misses;
			data.setTotalCount(total);

			if (total > 0) {
				final long percentHits = (hits * 100) / total;
				data.setHitsPercent(percentHits);
			}
		}

		return data;
	}

	private static String subKey(String key) {
		return " - " + key;
	}

	private void logStats() {

		// on récupère les noms des services
		final Set<String> keys = new HashSet<String>();
		synchronized (rawServices) {
			keys.addAll(rawServices.keySet());
		}
		synchronized (cachedServices) {
			keys.addAll(cachedServices.keySet());
		}

		if (keys.isEmpty()) {
			// rien à faire
			return;
		}

		// on trie les clés avant de les afficher
		List<String> sortedKeys = new ArrayList<String>(keys);
		Collections.sort(sortedKeys);

		// extrait et analyse les stats des services
		int maxLen = 0; // longueur maximale des clés
		long lastCallTime = 0;
		final List<StatsData> stats = new ArrayList<StatsData>(sortedKeys.size());
		for (String k : sortedKeys) {
			final StatsData data = get(k);
			stats.add(data);
			lastCallTime = Math.max(lastCallTime, data.getLastCallTime());
			maxLen = Math.max(maxLen, k.length());

			final Map<String, StatsData> subData = data.getDetailedData();
			for (Map.Entry<String, StatsData> e : subData.entrySet()) {
				maxLen = Math.max(maxLen, subKey(e.getKey()).length());
			}
		}

		// s'il n'y a eu aucun appel depuis la dernière fois, on ne log rien
		if (lastCallTime <= lastLoggedCallTime) {
			return;
		}
		lastLoggedCallTime = lastCallTime;

		StringBuilder b = new StringBuilder("Statistiques des services et caches:\n");
		b.append(StringUtils.repeat(" ", maxLen + 1));
		b.append(" |                     (raw)                  |                  (cache)\n");
		b.append(StringUtils.repeat(" ", maxLen + 1));
		b.append(" | ping (last 5 minutes) | ping (since start) | hits percent | hits count | total count\n");
		b.append(StringUtils.repeat("-", maxLen + 1));
		b.append("-+-----------------------+--------------------+--------------+------------+-------------\n");

		final int count = stats.size();
		for (int i = 0; i < count; ++i) {
			final String k = sortedKeys.get(i);
			final StatsData data = stats.get(i);
			b.append(printLine(maxLen, k, data));

			final Map<String, StatsData> subData = data.getDetailedData();
			for (Map.Entry<String, StatsData> e : subData.entrySet()) {
				b.append(printLine(maxLen, subKey(e.getKey()), e.getValue()));
			}
		}

		LOGGER.info(b.toString());
	}

	private String printLine(int maxLen, final String key, final StatsData data) {

		final StringBuilder b = new StringBuilder();

		final String recentPing = (data.getRecentPing() == null ? "-   " : String.format("%d ms", data.getRecentPing()));
		final String totalPing = (data.getTotalPing() == null ? "-   " : String.format("%d ms", data.getTotalPing()));
		final String hitPercent = (data.getHitsPercent() == null ? "-" : String.format("%d%%", data.getHitsPercent()));
		final String hitCount = (data.getHitsCount() == null ? "-" : String.format("%9d", data.getHitsCount()));
		final String totalCount = (data.getTotalCount() == null ? "-" : String.format("%9d", data.getTotalCount()));

		b.append(' ').append(lpad(key, maxLen)).append(" | ");
		b.append(lpad(recentPing, 21)).append(" | ");
		b.append(lpad(totalPing, 18)).append(" | ");
		b.append(lpad(hitPercent, 12)).append(" | ");
		b.append(lpad(hitCount, 10)).append(" | ");
		b.append(lpad(totalCount, 11));
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
		singleton = this;
		if (LOGGER.isInfoEnabled()) {
			timer.schedule(task, LOG_PERIODE, LOG_PERIODE);
		}
	}

	public void destroy() throws Exception {
		timer.cancel();
		singleton = null;
	}
}
