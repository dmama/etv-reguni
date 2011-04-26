package ch.vd.uniregctb.database;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.registre.base.dbutils.WrappingDataSource;
import ch.vd.uniregctb.interfaces.service.ServiceTracing;
import ch.vd.uniregctb.stats.StatsService;

/**
 * Datasource de tracing qui permet de relever les temps de réponses des requêtes JDBC.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TracingDataSource implements DataSource, InitializingBean, DisposableBean, WrappingDataSource {

	protected final Log LOGGER = LogFactory.getLog(TracingDataSource.class);

	public static final String SERVICE_NAME = "JDBC";

	private DataSource target;
	private ServiceTracing tracing = new ServiceTracing(SERVICE_NAME);
	private StatsService statsService;
	private boolean enabled;

	/**
	 * @param target La datasource réelle à tracer
	 */
	public void setTarget(DataSource target) {
		this.target = target;
	}

	public DataSource getTarget() {
		return target;
	}

	public DataSource getUltimateTarget() {
		if (target != null && target instanceof WrappingDataSource) {
			return ((WrappingDataSource) target).getUltimateTarget();
		}
		else {
			return target;
		}
	}

	/**
	 * @param enabled <i>vrai</i> pour activer le tracing; <i>faux</i> pour simplement déléguer les appels à la target.
	 */
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void afterPropertiesSet() throws Exception {
		statsService.registerService(SERVICE_NAME, tracing);
	}

	public void destroy() throws Exception {
		statsService.unregisterService(SERVICE_NAME);
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection getConnection() throws SQLException {
		if (enabled) {
			return new TracingConnection(target.getConnection(), tracing);
		}
		else {
			return target.getConnection();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public Connection getConnection(String username, String password) throws SQLException {
		if (enabled) {
			return new TracingConnection(target.getConnection(username, password), tracing);
		}
		else {
			return target.getConnection(username, password);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public PrintWriter getLogWriter() throws SQLException {
		return target.getLogWriter();
	}

	/**
	 * {@inheritDoc}
	 */
	public int getLoginTimeout() throws SQLException {
		return target.getLoginTimeout();
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLogWriter(PrintWriter out) throws SQLException {
		target.setLogWriter(out);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setLoginTimeout(int seconds) throws SQLException {
		target.setLoginTimeout(seconds);
	}

	@Override
	public <T> T unwrap(Class<T> iface) throws SQLException {
		return target.unwrap(iface);
	}

	@Override
	public boolean isWrapperFor(Class<?> iface) throws SQLException {
		return target.isWrapperFor(iface);
	}
}