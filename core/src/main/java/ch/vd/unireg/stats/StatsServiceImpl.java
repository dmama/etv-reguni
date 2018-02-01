package ch.vd.unireg.stats;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.asciiart.table.AlignMode;
import ch.vd.registre.asciiart.table.Cell;
import ch.vd.registre.asciiart.table.Column;
import ch.vd.registre.asciiart.table.Header;
import ch.vd.registre.asciiart.table.Options;
import ch.vd.registre.asciiart.table.Row;
import ch.vd.registre.asciiart.table.Table;
import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.cache.CacheStats;
import ch.vd.unireg.cache.UniregCacheInterface;
import ch.vd.unireg.common.TimeHelper;

public class StatsServiceImpl implements InitializingBean, DisposableBean, StatsService, StatsExposureInterface {

	private static final Logger LOGGER = LoggerFactory.getLogger(StatsServiceImpl.class);

	private static final String CR = System.lineSeparator();

	private static final long UNE_MINUTE = 60000L;
	private int logPeriode = 5; // 5 * UNE_MINUTE

	private final Timer timer = new Timer("StatsServiceTicking", true);
	private final Map<String, ServiceTracingInterface> rawServices = new HashMap<>();
	private final Map<String, UniregCacheInterface> cachedServices = new HashMap<>();
	private final Map<String, LoadMonitor> loadMonitors = new HashMap<>();
	private final Map<String, JobMonitor> jobMonitors = new HashMap<>();
	private long lastLoggedCallTime = 0;

	/**
	 * @param logPeriode la période de logging des statistiques des caches (en minutes, 0 pour désactiver le logging)
	 */
	public void setLogPeriode(int logPeriode) {
		this.logPeriode = logPeriode;
	}

	private final class TickingTask extends TimerTask {

		/**
		 * Compteur d'appels à la méthode {@link #run} : on loggue en plus les statistiques tous les {@link #logPeriode} appels
		 */
		private int compteur = 0;

		@Override
		public void run() {

			// on loggue d'abord les stats avant de faire glisser
			if (logPeriode > 0) {
				compteur = (compteur + 1) % logPeriode;
				if (compteur == 0 && LOGGER.isInfoEnabled()) {
					try {
						logStats();
					}
					catch (Exception e) {
						LOGGER.warn("Le log des statistiques a renvoyé une exception", e);
					}
				}
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
	public void registerJobMonitor(String jobName, JobMonitor job) {
		synchronized (jobMonitors) {
			jobMonitors.put(jobName, job);
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

	@Override
	public void unregisterJobMonitor(String jobName) {
		synchronized (jobMonitors) {
			jobMonitors.remove(jobName);
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
		final Set<String> keys = new HashSet<>();
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

		// calcul des affichages des statistiques
		final String[] stats = {
				buildCacheStats(),
				buildServiceStats(),
				buildLoadMonitorStats(),
				buildLoadMonitorDetails(),
				buildJobMonitorStats()
		};

		// concaténation des affichages des statistiques non-vides
		boolean atLeastOne = false;
		final StringBuilder b = new StringBuilder("Statistiques des caches et services:").append(CR).append(CR);
		for (String stat : stats) {
			if (StringUtils.isNotBlank(stat)) {
				b.append(stat).append(CR);
				atLeastOne = true;
			}
		}
		return atLeastOne ? b.append("----").toString() : StringUtils.EMPTY;
	}

	private String buildCacheStats() {

		// on récupère les noms des caches (triés)
		final Set<String> keys = new TreeSet<>();
		synchronized (cachedServices) {
			keys.addAll(cachedServices.keySet());
		}

		// construit la table ascii-art qui contiendra les statistiques à afficher
		final List<Header> headers = new ArrayList<>();
		headers.add(new Header(new Column("Caches", AlignMode.LEFT), new Column("hits percent"), new Column("hits count"), new Column("total count"), new Column("time-to-idle"),
				new Column("time-to-live"), new Column("max elements")));
		final Table table = new Table(new Options(false), headers);

		// extrait et analyse les stats des services
		for (String k : keys) {
			final CacheStats data = getCacheStats(k);
			table.addRow(buildRow(k, data));
		}

		return table.toString() + CR;
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

		// on récupère les noms des services (triés)
		final Set<String> keys = new TreeSet<>();
		synchronized (rawServices) {
			keys.addAll(rawServices.keySet());
		}

		// construit la table ascii-art qui contiendra les statistiques à afficher
		final List<Header> headers = new ArrayList<>();
		headers.add(new Header(new Column(""), new Column("(last 5 minutes)", AlignMode.CENTER, 4), new Column("(since start)", AlignMode.CENTER, 4)));
		headers.add(new Header(new Column("Services", AlignMode.LEFT), new Column("ping"), new Column("ping/item"), new Column("hits count"), new Column("items count"),
				new Column("ping"), new Column("ping/item"), new Column("hits count"), new Column("items count")));
		final Table table = new Table(new Options(false), headers);

		// extrait et analyse les stats des services
		for (String k : keys) {
			final ServiceStats data = getServiceStats(k);
			table.addRow(buildRow(k, data));

			final Map<String, ServiceStats> subData = data.getDetailedData();
			for (Map.Entry<String, ServiceStats> e : subData.entrySet()) {
				table.addRow(buildRow(subKey(e.getKey()), e.getValue()));
			}
		}

		return table.toString() + CR;
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

		// on récupère les noms des services (triés)
		final Set<String> keys = new TreeSet<>();
		synchronized (loadMonitors) {
			keys.addAll(loadMonitors.keySet());
		}

		if (keys.isEmpty()) {
			return StringUtils.EMPTY;
		}

		// construit la table ascii-art qui contiendra les statistiques à afficher
		final List<Header> headers = new ArrayList<>();
		headers.add(new Header(new Column("Load", AlignMode.LEFT), new Column("current"), new Column("5-min average")));
		final Table table = new Table(new Options(false), headers);

		// extrait et analyse les stats des services
		for (String k : keys) {
			final LoadMonitorStats data = getLoadMonitorStats(k);
			table.addRow(buildRow(k, data));
		}

		return table.toString() + CR;
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

	private String buildLoadMonitorDetails() {

		// on veut trier les services par ordre alphabétique, d'où la TreeMap
		final Map<String, List<LoadDetail>> allDetails = new TreeMap<>();

		// on récupère les noms des services et les données
		synchronized (loadMonitors) {
			for (Map.Entry<String, LoadMonitor> entry : loadMonitors.entrySet()) {
				final LoadMonitorable monitorable = entry.getValue().getMonitorable();
				if (monitorable.getLoad() > 0 && monitorable instanceof DetailedLoadMonitorable) {
					final DetailedLoadMonitorable detailedMonitorable = (DetailedLoadMonitorable) monitorable;
					final List<LoadDetail> details = detailedMonitorable.getLoadDetails();
					if (!details.isEmpty()) {
						allDetails.put(entry.getKey(), details);
					}
				}
			}
		}

		if (allDetails.isEmpty()) {
			return StringUtils.EMPTY;
		}

		// construction d'une table ascii-art qui contiendra les détails
		final List<Header> headers = new ArrayList<>();
		headers.add(new Header(new Column("Load details", AlignMode.LEFT), new Column("thread"), new Column("duration"), new Column("description", AlignMode.LEFT)));
		final Table table = new Table(new Options(false), headers);

		// on va vouloir trier les processus en cours depuis le plus ancien vers le plus récent
		// i.e. dans l'ordre décroissant de leur durée
		final Comparator<LoadDetail> comparator = Comparator.comparing(LoadDetail::getDuration, Comparator.reverseOrder());

		// log les appels en cours sur les services monitorés qui le supportent
		for (Map.Entry<String, List<LoadDetail>> entry : allDetails.entrySet()) {
			final List<LoadDetail> details = entry.getValue();
			details.sort(comparator);
			for (LoadDetail detail : details) {
				final Row row = new Row();
				row.addCell(new Cell(entry.getKey()));
				row.addCell(new Cell(detail.getThreadName()));
				row.addCell(new Cell(TimeHelper.formatDureeShort(detail.getDuration()), AlignMode.RIGHT));
				row.addCell(new Cell(detail.getDescription()));
				table.addRow(row);
			}
		}

		return table.toString() + CR;
	}

	private String buildJobMonitorStats() {

		class JobData {
			final Integer progress;
			final Date start;
			final String status;

			JobData(Date start, Integer progress, String status) {
				this.progress = progress;
				this.status = status;
				this.start = start;
			}
		}

		final Map<String, JobData> data = new TreeMap<>();
		synchronized (jobMonitors) {
			for (Map.Entry<String, JobMonitor> entry : jobMonitors.entrySet()) {
				final JobMonitor monitor = entry.getValue();
				final Date start = monitor.getStartDate();
				if (start != null) {
					final String status = monitor.getRunningMessage();
					final Integer progress = monitor.getPercentProgression();
					data.put(entry.getKey(), new JobData(start, progress, status));
				}
			}
		}

		if (data.isEmpty()) {
			return StringUtils.EMPTY;
		}

		final long nowts = DateHelper.getCurrentDate().getTime();
		final List<Header> headers = new ArrayList<>();
		headers.add(new Header(new Column("Running jobs", AlignMode.LEFT), new Column("start", AlignMode.LEFT), new Column("duration"), new Column("progress"), new Column("status", AlignMode.LEFT)));
		final Table table = new Table(new Options(false), headers);
		for (Map.Entry<String, JobData> entry : data.entrySet()) {
			final Row row = new Row();
			row.addCell(new Cell(entry.getKey()));

			final JobData jobData = entry.getValue();
			row.addCell(new Cell(DateHelper.dateTimeToDisplayString(jobData.start)));
			row.addCell(new Cell(TimeHelper.formatDureeShort(nowts - jobData.start.getTime()), AlignMode.RIGHT));
			row.addCell(new Cell(jobData.progress != null ? String.format("%d %%", jobData.progress) : StringUtils.EMPTY, AlignMode.RIGHT));
			row.addCell(new Cell(jobData.status));
			table.addRow(row);
		}

		return table.toString() + CR;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		timer.schedule(new TickingTask(), UNE_MINUTE, UNE_MINUTE);
	}

	@Override
	public void destroy() throws Exception {
		timer.cancel();
	}

	@Override
	public Map<String, ServiceTracingInterface> getServices() {
		synchronized (rawServices) {
			return new HashMap<>(rawServices);
		}
	}

	@Override
	public Map<String, UniregCacheInterface> getCaches() {
		synchronized (cachedServices) {
			return new HashMap<>(cachedServices);
		}
	}

	@Override
	public Map<String, LoadMonitor> getLoadMonitors() {
		synchronized (loadMonitors) {
			return new HashMap<>(loadMonitors);
		}
	}

	@Override
	public Map<String, JobMonitor> getJobMonitors() {
		synchronized (jobMonitors) {
			return new HashMap<>(jobMonitors);
		}
	}
}
