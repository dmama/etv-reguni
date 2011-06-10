package ch.vd.uniregctb.datasource;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean spécialisé qui instancie dynamiquement à une datasource Oracle ou PostgreSQL en fonction du profile Jdbc.
 */
public class DynamicDataSource implements FactoryBean, InitializingBean, DisposableBean {

	private String jdbcProfile;

	private String oracleDriverClassName;
	private String oracleUrl;
	private String oracleUsername;
	private String oraclePassword;

	private String postgresqlDriverClassName;
	private String postgresqlUrl;
	private String postgresqlUsername;
	private String postgresqlPassword;

	private BasicDataSource instance;

	@Override
	public Object getObject() throws Exception {
		return instance;
	}

	@Override
	public Class getObjectType() {
		return BasicDataSource.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		if (jdbcProfile.equalsIgnoreCase("oracle")) {
			final UniregDataSource i = new UniregDataSource();
			i.setDriverClassName(oracleDriverClassName);
			i.setUrl(oracleUrl);
			i.setUsername(oracleUsername);
			i.setPassword(oraclePassword);
			i.setInitialSize(1);
			i.setMaxActive(1);
			instance = i;
		}
		else if (jdbcProfile.equalsIgnoreCase("postgresql")) {
			final BasicDataSource i = new BasicDataSource();
			i.setDriverClassName(postgresqlDriverClassName);
			i.setUrl(postgresqlUrl);
			i.setUsername(postgresqlUsername);
			i.setPassword(postgresqlPassword);
			i.setInitialSize(1);
			i.setMaxActive(1);
			instance = i;
		}
		else {
			throw new RuntimeException("Type de profile jdbc inconnu = [" + jdbcProfile + "]");
		}

		if (instance instanceof InitializingBean) {
			((InitializingBean) instance).afterPropertiesSet();
		}
	}

	@Override
	public void destroy() throws Exception {
		if (instance != null) {
			instance.close();
		}
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
