package ch.vd.uniregctb.datasource;

import org.apache.commons.beanutils.PropertyUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.tranql.connector.jdbc.AbstractXADataSourceMCF;

/**
 * Bean spécialisé qui instancie dynamiquement à une XAMCF Oracle ou PostgreSQL en fonction du profile Jdbc. 
 */
public class DynamicXAMCF implements FactoryBean, InitializingBean, DisposableBean {

	private String jdbcProfile;

	private String oracleServerName;
	private Integer oraclePortNumber;
	private String oracleDatabaseName;
	private String oracleUserName;
	private String oraclePassword;

	private String postgresqlServerName;
	private Integer postgresqlPortNumber;
	private String postgresqlDatabaseName;
	private String postgresqlUserName;
	private String postgresqlPassword;

	private AbstractXADataSourceMCF<?> instance;

	@Override
	public Object getObject() throws Exception {
		return instance;
	}

	@Override
	public Class getObjectType() {
		return AbstractXADataSourceMCF.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		
		if (jdbcProfile.equalsIgnoreCase("oracle")) {
			final UniregXAMCF i = new UniregXAMCF();
			i.setDriverType("thin");
			i.setServerName(oracleServerName);
			i.setPortNumber(oraclePortNumber);
			i.setDatabaseName(oracleDatabaseName);
			i.setUserName(oracleUserName);
			i.setPassword(oraclePassword);
			instance = i;
		}
		else if (jdbcProfile.equalsIgnoreCase("postgresql")) {
			final Class<?> clazz = Class.forName("org.tranql.connector.postgresql.PGXAMCF"); // instanciation dynamique pour éviter des erreurs de compile lorsque le profile 'postgresql' n'est pas spécifié
			final AbstractXADataSourceMCF<?> i = (AbstractXADataSourceMCF<?>) clazz.newInstance();
			PropertyUtils.setProperty(i, "serverName", postgresqlServerName);
			PropertyUtils.setProperty(i, "portNumber", postgresqlPortNumber);
			PropertyUtils.setProperty(i, "databaseName", postgresqlDatabaseName);
			PropertyUtils.setProperty(i, "userName", postgresqlUserName);
			PropertyUtils.setProperty(i, "password", postgresqlPassword);
			instance = i;
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
	public void setOracleServerName(String oracleServerName) {
		this.oracleServerName = oracleServerName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOraclePortNumber(Integer oraclePortNumber) {
		this.oraclePortNumber = oraclePortNumber;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOracleDatabaseName(String oracleDatabaseName) {
		this.oracleDatabaseName = oracleDatabaseName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOracleUserName(String oracleUserName) {
		this.oracleUserName = oracleUserName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOraclePassword(String oraclePassword) {
		this.oraclePassword = oraclePassword;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlServerName(String postgresqlServerName) {
		this.postgresqlServerName = postgresqlServerName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlPortNumber(Integer postgresqlPortNumber) {
		this.postgresqlPortNumber = postgresqlPortNumber;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlDatabaseName(String postgresqlDatabaseName) {
		this.postgresqlDatabaseName = postgresqlDatabaseName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlUserName(String postgresqlUserName) {
		this.postgresqlUserName = postgresqlUserName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlPassword(String postgresqlPassword) {
		this.postgresqlPassword = postgresqlPassword;
	}
}
