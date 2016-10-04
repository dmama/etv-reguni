package ch.vd.uniregctb.datasource;

import javax.sql.XADataSource;

import oracle.jdbc.xa.client.OracleXADataSource;
import org.postgresql.xa.PGXADataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean spécialisé qui instancie dynamiquement à une datasource Oracle ou PostgreSQL en fonction du profile Jdbc.
 */
public class DynamicDataSource implements FactoryBean<XADataSource>, InitializingBean, DisposableBean {

	private String jdbcProfile;

	private String oracleDriverClassName;
	private String oracleUrl;
	private String oracleUsername;
	private String oraclePassword;

	private String postgresqlDriverClassName;
	private String postgresqlUrl;
	private String postgresqlUsername;
	private String postgresqlPassword;

	private XADataSource instance;

	@Override
	public XADataSource getObject() throws Exception {
		return instance;
	}

	@Override
	public Class<?> getObjectType() {
		return XADataSource.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		if (jdbcProfile.equalsIgnoreCase("oracle")) {
			final OracleXADataSource ds = new OracleXADataSource();
			ds.setURL(oracleUrl);
			ds.setUser(oracleUsername);
			ds.setPassword(oraclePassword);
			instance = ds;
		}
		else if (jdbcProfile.equalsIgnoreCase("postgresql")) {
			final PGXADataSource ds = new PGXADataSource();
			ds.setUrl(postgresqlUrl);
			ds.setUser(postgresqlUsername);
			ds.setPassword(postgresqlPassword);
			instance = ds;
		}
		else {
			throw new RuntimeException("Type de profile jdbc inconnu = [" + jdbcProfile + ']');
		}

		if (instance instanceof InitializingBean) {
			((InitializingBean) instance).afterPropertiesSet();
		}
	}

	@Override
	public void destroy() throws Exception {
		if (instance instanceof DisposableBean) {
			((DisposableBean) instance).destroy();
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setJdbcProfile(String jdbcProfile) {
		this.jdbcProfile = jdbcProfile;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOracleDriverClassName(String oracleDriverClassName) {
		this.oracleDriverClassName = oracleDriverClassName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOracleUrl(String oracleUrl) {
		this.oracleUrl = oracleUrl;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOracleUsername(String oracleUsername) {
		this.oracleUsername = oracleUsername;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOraclePassword(String oraclePassword) {
		this.oraclePassword = oraclePassword;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlDriverClassName(String postgresqlDriverClassName) {
		this.postgresqlDriverClassName = postgresqlDriverClassName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlUrl(String postgresqlUrl) {
		this.postgresqlUrl = postgresqlUrl;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlUsername(String postgresqlUsername) {
		this.postgresqlUsername = postgresqlUsername;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlPassword(String postgresqlPassword) {
		this.postgresqlPassword = postgresqlPassword;
	}
}
