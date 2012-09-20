package ch.vd.uniregctb.common;

import org.apache.log4j.Logger;
import org.hibernate.dialect.Dialect;
import org.springframework.beans.factory.InitializingBean;

public class SpringPropertiesLogger implements InitializingBean {

	public static final Logger LOGGER = Logger.getLogger(SpringPropertiesLogger.class);

	private final String title;
	private Dialect hibernateDialect;
	private String jdbcDriverClassName;
	private String jdbcUrl;
	private String jdbcUsername;
	private String properties;

	public SpringPropertiesLogger(String title) {
		this.title = title;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateDialect(Dialect hibernateDialect) {
		this.hibernateDialect = hibernateDialect;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setJdbcDriverClassName(String jdbcDriverClassName) {
		this.jdbcDriverClassName = jdbcDriverClassName;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setJdbcUrl(String jdbcUrl) {
		this.jdbcUrl = jdbcUrl;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setJdbcUsername(String jdbcUsername) {
		this.jdbcUsername = jdbcUsername;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setProperties(String properties) {
		this.properties = properties;
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		// Change les | en \n (EOL)
		properties = properties.replace('|', '\n');

		String str = "\n";
		str += "=== Begin Dump of Spring properties ===";
		str += "=== " + title + " ===";
		str += "\n Hibernate dialect       : " + hibernateDialect.getClass().getName();
		str += "\n JDBC driver             : " + jdbcDriverClassName;
		str += "\n JDBC url                : " + jdbcUrl;
		str += "\n UniregCTB DB user       : " + jdbcUsername;
		str += properties;
		str += "\n=== End Dump of Spring properties ===";
		str += "\n";

		LOGGER.info(str);
	}
}
