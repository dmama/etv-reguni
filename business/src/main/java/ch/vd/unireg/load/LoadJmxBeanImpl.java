package ch.vd.unireg.load;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.unireg.stats.LoadMonitorable;
import ch.vd.unireg.stats.StatsService;

@ManagedResource
public class LoadJmxBeanImpl<T extends LoadMonitorable> implements LoadJmxBean, DisposableBean {

	/**
	 * Le service à monitorer
	 */
	protected final T service;

	/**
	 * Le nom du service à monitorer
	 */
	private final String serviceName;

	/**
	 * Calculateur de charge moyenne sur les 5 dernières minutes
	 */
	private final LoadAverager averager;

	/**
	 * Service de statistiques pour les logs
	 */
	private final StatsService statsService;

	public LoadJmxBeanImpl(String serviceName, T service, @Nullable StatsService statsService) {
		this.service = service;
		this.serviceName = serviceName;
		this.statsService = statsService;

		this.averager = new LoadAverager(service, serviceName, 600, 500);        // 5 minutes, 2 fois par seconde
		this.averager.start();
		
		// on n'enregistre dans le service de stats que si c'est nécessaire
		if (statsService != null) {
			statsService.registerLoadMonitor(serviceName, new BasicLoadMonitor(service, averager));
		}
	}

	@Override
	public void destroy() throws Exception {
		averager.stop();
		if (statsService != null) {
			statsService.unregisterLoadMonitor(serviceName);
		}
	}

	@Override
	@ManagedAttribute
	public int getLoad() {
		return service.getLoad();
	}

	@Override
	@ManagedAttribute
	public double getAverageLoad() {
		return averager.getAverageLoad();
	}
}
