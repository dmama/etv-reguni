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

import ch.vd.registre.asciiart.table.AlignMode;
import ch.vd.registre.asciiart.table.Cell;
import ch.vd.registre.asciiart.table.Column;
import ch.vd.registre.asciiart.table.Header;
import ch.vd.registre.asciiart.table.Options;
import ch.vd.registre.asciiart.table.Row;
import ch.vd.registre.asciiart.table.Table;
import ch.vd.uniregctb.cache.CacheStats;
import ch.vd.uniregctb.cache.UniregCacheInterface;
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

		// construit la table ascii-art qui contiendra les statistiques à afficher
		final List<Header> headers = new ArrayList<Header>();
		headers.add(new Header(new Column("Caches", AlignMode.LEFT), new Column("hits percent"), new Column("hits count"), new Column("total count"), new Column("time-to-idle"),
				new Column("time-to-live"), new Column("max elements")));
		final Table table = new Table(new Options(false), headers);

		// on trie les clés avant de les afficher
		final List<String> sortedKeys = new ArrayList<String>(keys);
		Collections.sort(sortedKeys);

		// extrait et analyse les stats des services
		for (String k : sortedKeys) {
			final CacheStats data = getCacheStats(k);
			table.addRow(buildRow(k, data));
		}

		return table.toString() + "\n";
	}

	private static Row buildRow(String key, CacheStats data) {
		Row row = new Row();

		row.addCell(new Cell(key));

		final String hitPercent = (data.getHitsPercent() == null ? "-" : String.format("%d%%", data.getHitsPercent()));
		row.addCell(new Cell(hitPercent, AlignMode.RIGHT));

		final String hitCount = String.format("%9d", data.getHitsCount());
		row.addCell(new Cell(hitCount, AlignMode.RIGHT));

		final String totalCount = String.format("%9d", data.getTotalCount());
		row.addCell(new Cell(totalCount, AlignMode.RIGHT));

		final String timeToIdle = (data.getTimeToIdle() == null ? "-" : String.valueOf(data.getTimeToIdle()));
		row.addCell(new Cell(timeToIdle, AlignMode.RIGHT));

		final String timeToLive = (data.getTimeToLive() == null ? "-" : String.valueOf(data.getTimeToLive()));
		row.addCell(new Cell(timeToLive, AlignMode.RIGHT));

		final String maxElements = (data.getMaxElements() == null ? "-" : String.valueOf(data.getMaxElements()));
		row.addCell(new Cell(maxElements, AlignMode.RIGHT));

		return row;
	}

	public String buildServiceStats() {

		// on récupère les noms des services
		final Set<String> keys = new HashSet<String>();
		synchronized (rawServices) {
			keys.addAll(rawServices.keySet());
		}

		// construit la table ascii-art qui contiendra les statistiques à afficher
		final List<Header> headers = new ArrayList<Header>();
		headers.add(new Header(new Column(""), new Column("(last 5 minutes)", AlignMode.CENTER, 4), new Column("(since start)", AlignMode.CENTER, 4)));
		headers.add(new Header(new Column("Services", AlignMode.LEFT), new Column("ping"), new Column("ping/item"), new Column("hits count"), new Column("items count"),
				new Column("ping"), new Column("ping/item"), new Column("hits count"), new Column("items count")));
		final Table table = new Table(new Options(false), headers);

		// on trie les clés avant de les afficher
		List<String> sortedKeys = new ArrayList<String>(keys);
		Collections.sort(sortedKeys);

		// extrait et analyse les stats des services
		for (String k : sortedKeys) {
			final ServiceStats data = getServiceStats(k);
			table.addRow(buildRow(k, data));

			final Map<String, ServiceStats> subData = data.getDetailedData();
			for (Map.Entry<String, ServiceStats> e : subData.entrySet()) {
				table.addRow(buildRow(subKey(e.getKey()), e.getValue()));
			}
		}

		return table.toString() + "\n";
	}

	private static Row buildRow(String key, ServiceStats data) {
		Row row = new Row();

		row.addCell(new Cell(key));

		final String recentPing = String.format("%d ms", data.getRecentPing());
		row.addCell(new Cell(recentPing, AlignMode.RIGHT));

		final String recentItemsPing = data.getRecentItemsPing() == null ? "" : String.format("(%d ms/item)", data.getRecentItemsPing());
		row.addCell(new Cell(recentItemsPing, AlignMode.RIGHT));

		final String recentCount = String.format("%d", data.getRecentCount());
		row.addCell(new Cell(recentCount, AlignMode.RIGHT));

		final String recentItemsCount = data.getRecentItemsCount() == null ? "" : String.format("(%d items)", data.getRecentItemsCount());
		row.addCell(new Cell(recentItemsCount, AlignMode.RIGHT));

		final String totalPing = String.format("%d ms", data.getTotalPing());
		row.addCell(new Cell(totalPing, AlignMode.RIGHT));

		final String totalItemsPing = data.getTotalItemsPing() == null ? "" : String.format("(%d ms/item)", data.getTotalItemsPing());
		row.addCell(new Cell(totalItemsPing, AlignMode.RIGHT));

		final String totalCount = String.format("%d", data.getTotalCount());
		row.addCell(new Cell(totalCount, AlignMode.RIGHT));

		final String totalItemsCount = data.getTotalItemsCount() == null ? "" : String.format("(%d items)", data.getTotalItemsCount());
		row.addCell(new Cell(totalItemsCount, AlignMode.RIGHT));

		return row;
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

		// construit la table ascii-art qui contiendra les statistiques à afficher
		final List<Header> headers = new ArrayList<Header>();
		headers.add(new Header(new Column("Load", AlignMode.LEFT), new Column("current"), new Column("5-min average")));
		final Table table = new Table(new Options(false), headers);

		// on trie les clés avant de les afficher
		final List<String> sortedKeys = new ArrayList<String>(keys);
		Collections.sort(sortedKeys);

		// extrait et analyse les stats des services
		for (String k : sortedKeys) {
			final LoadMonitorStats data = getLoadMonitorStats(k);
			table.addRow(buildRow(k, data));
		}

		return table.toString() + "\n";
	}

	private static Row buildRow(String key, LoadMonitorStats data) {

		Row row = new Row();

		row.addCell(new Cell(key));

		final String chargeInstantanee = String.format("%d", data.getChargeInstantanee());
		row.addCell(new Cell(chargeInstantanee, AlignMode.RIGHT));

		final String chargeMoyenne = String.format("%#13.3f", data.getMoyenneCharge());
		row.addCell(new Cell(chargeMoyenne, AlignMode.RIGHT));

		return row;
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
