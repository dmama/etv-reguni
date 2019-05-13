package ch.vd.unireg.database;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.unireg.stats.ServiceTracing;
import ch.vd.unireg.stats.StatsService;

/**
 * Datasource de tracing qui permet de relever les temps de réponses des requêtes JDBC.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TracingDataSource extends ch.vd.shared.tracing.datasource.TracingDataSource implements InitializingBean, DisposableBean {

	public static final String SERVICE_NAME = "JDBC";

	private final ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);
	private StatsService statsService;

	public TracingDataSource() {
		setCollector(tracing);
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		statsService.registerService(SERVICE_NAME, tracing);
	}

	@Override
	public void destroy() throws Exception {
		statsService.unregisterService(SERVICE_NAME);
	}
}