package ch.vd.uniregctb.datasource;

import org.hibernate.dialect.Dialect;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean spécialisé qui instancie dynamiquement à une dialect hibernate Oracle ou PostgreSQL en fonction du profile Jdbc. 
 */
public class DynamicHibernateDialect implements FactoryBean, InitializingBean {

	private String jdbcProfile;

	private Class<?> oracleDialectClass;
	private Class<?> postgresqlDialectClass;
	private Dialect instance;

	@Override
	public Object getObject() throws Exception {
		return instance;
	}

	@Override
	public Class getObjectType() {
		return Dialect.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		if (jdbcProfile.equalsIgnoreCase("oracle")) {
			instance = (Dialect) oracleDialectClass.newInstance();
		}
		else if (jdbcProfile.equalsIgnoreCase("postgresql")) {
			instance = (Dialect) postgresqlDialectClass.newInstance();
		}
		else {
			throw new RuntimeException("Type de profile jdbc inconnu = [" + jdbcProfile + "]");
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setJdbcProfile(String jdbcProfile) {
		this.jdbcProfile = jdbcProfile;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setOracleDialectClass(Class<?> oracleDialectClass) {
		this.oracleDialectClass = oracleDialectClass;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlDialectClass(Class<?> postgresqlDialectClass) {
		this.postgresqlDialectClass = postgresqlDialectClass;
	}
}
