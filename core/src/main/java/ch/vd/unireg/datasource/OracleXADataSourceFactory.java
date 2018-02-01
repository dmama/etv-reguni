package ch.vd.unireg.datasource;

import javax.sql.XADataSource;
import java.util.Properties;

import oracle.jdbc.OracleDriver;
import oracle.jdbc.xa.client.OracleXADataSource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

public class OracleXADataSourceFactory implements FactoryBean<XADataSource>, InitializingBean {

	private String driverType;
	private String serverName;
	private int portNumber;
	private String databaseName;
	private String serviceName;
	private String userName;
	private String password;

	private XADataSource dataSource;

	public void setDriverType(String driverType) {
		this.driverType = driverType;
	}

	public void setServerName(String serverName) {
		this.serverName = serverName;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public void setDatabaseName(String databaseName) {
		// le trimToNull est nécessaire pour permettre l'utilisation d'un nom d'instance <b>ou</b> d'un nom de service
		this.databaseName = StringUtils.trimToNull(databaseName);
	}

	public void setServiceName(String serviceName) {
		// le trimToNull est nécessaire pour permettre l'utilisation d'un nom d'instance <b>ou</b> d'un nom de service
		this.serviceName = StringUtils.trimToNull(serviceName);
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		final OracleXADataSource ds = new OracleXADataSource();
		ds.setDriverType(driverType);
		ds.setServerName(serverName);
		ds.setPortNumber(portNumber);
		ds.setDatabaseName(databaseName);
		ds.setServiceName(serviceName);
		ds.setUser(userName);
		ds.setPassword(password);

		final Properties props = new Properties();
		props.setProperty(OracleDriver.defaultncharprop_string, "true");
		ds.setConnectionProperties(props);

		this.dataSource = ds;
	}

	@Override
	public XADataSource getObject() throws Exception {
		return dataSource;
	}

	@Override
	public Class<?> getObjectType() {
		return XADataSource.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
