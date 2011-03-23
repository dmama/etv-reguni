package ch.vd.uniregctb.datasource;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean spécialisé qui retourne une propriété spécifique à Oracle ou à PostgreSQL en fonction du profile Jdbc. 
 */
public class DynamicString implements FactoryBean, InitializingBean {

	private String jdbcProfile;

	private String oracleProperty;
	private String postgresqlProperty;

	private String instance;

	public Object getObject() throws Exception {
		return instance;
	}

	public Class getObjectType() {
		return String.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() throws Exception {

		if (jdbcProfile.equalsIgnoreCase("oracle")) {
			instance = oracleProperty;
		}
		else if (jdbcProfile.equalsIgnoreCase("postgresql")) {
			instance = postgresqlProperty;
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
	public void setOracleProperty(String oracleProperty) {
		this.oracleProperty = oracleProperty;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setPostgresqlProperty(String postgresqlProperty) {
		this.postgresqlProperty = postgresqlProperty;
	}
}
