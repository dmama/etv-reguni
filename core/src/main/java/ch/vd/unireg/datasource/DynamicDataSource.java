package ch.vd.unireg.datasource;

import javax.sql.XADataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Bean spécialisé qui instancie dynamiquement à une datasource Oracle ou PostgreSQL en fonction du profile Jdbc.
 */
public class DynamicDataSource implements FactoryBean<XADataSource>, InitializingBean, DisposableBean {

	private String jdbcProfile;

	private String oracleDataSourceClassName;
	private String oracleUrl;
	private String oracleUsername;
	private String oraclePassword;

	private String postgresqlDataSourceClassName;
	private String postgresqlUrl;
	private String postgresqlUsername;
	private String postgresqlPassword;

	private String h2DataSourceClassName;
	private String h2Url;
	private String h2Username;
	private String h2Password;

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
			final Class<? extends XADataSource> clazz = (Class<? extends XADataSource>) Class.forName(oracleDataSourceClassName);
			final XADataSource ds = clazz.getDeclaredConstructor().newInstance();
			setConnectionProperties(ds, "setURL", oracleUrl, "setUser", oracleUsername, "setPassword", oraclePassword);
			instance = ds;
		}
		else if (jdbcProfile.equalsIgnoreCase("postgresql")) {
			final Class<? extends XADataSource> clazz = (Class<? extends XADataSource>) Class.forName(postgresqlDataSourceClassName);
			final XADataSource ds = clazz.getDeclaredConstructor().newInstance();
			setConnectionProperties(ds, "setUrl", postgresqlUrl, "setUser", postgresqlUsername, "setPassword", postgresqlPassword);
			instance = ds;
		}
		else if (jdbcProfile.equalsIgnoreCase("h2")) {
			final Class<? extends XADataSource> clazz = (Class<? extends XADataSource>) Class.forName(h2DataSourceClassName);
			final XADataSource ds = clazz.getDeclaredConstructor().newInstance();
			setConnectionProperties(ds, "setUrl", h2Url, "setUser", h2Username, "setPassword", h2Password);
			instance = ds;
		}
		else {
			throw new RuntimeException("Type de profile jdbc inconnu = [" + jdbcProfile + ']');
		}

		if (instance instanceof InitializingBean) {
			((InitializingBean) instance).afterPropertiesSet();
		}
	}

	private static void setConnectionProperties(XADataSource dataSource, String urlSetterName, String url, String userSetterName, String user, String passwordSetterName, String password) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException {
		invokeSetter(dataSource, urlSetterName, String.class, url);
		invokeSetter(dataSource, userSetterName, String.class, user);
		invokeSetter(dataSource, passwordSetterName, String.class, password);
	}

	private static <T> void invokeSetter(Object target, String setterName, Class<T> attributeType, T attributeValue) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
		final Method method = target.getClass().getMethod(setterName, attributeType);
		method.invoke(target, attributeValue);
	}

	@Override
	public void destroy() throws Exception {
		if (instance instanceof DisposableBean) {
			((DisposableBean) instance).destroy();
		}
	}

	public void setJdbcProfile(String jdbcProfile) {
		this.jdbcProfile = jdbcProfile;
	}

	public void setOracleDataSourceClassName(String oracleDataSourceClassName) {
		this.oracleDataSourceClassName = oracleDataSourceClassName;
	}

	public void setOracleUrl(String oracleUrl) {
		this.oracleUrl = oracleUrl;
	}

	public void setOracleUsername(String oracleUsername) {
		this.oracleUsername = oracleUsername;
	}

	public void setOraclePassword(String oraclePassword) {
		this.oraclePassword = oraclePassword;
	}

	public void setPostgresqlDataSourceClassName(String postgresqlDataSourceClassName) {
		this.postgresqlDataSourceClassName = postgresqlDataSourceClassName;
	}

	public void setPostgresqlUrl(String postgresqlUrl) {
		this.postgresqlUrl = postgresqlUrl;
	}

	public void setPostgresqlUsername(String postgresqlUsername) {
		this.postgresqlUsername = postgresqlUsername;
	}

	public void setPostgresqlPassword(String postgresqlPassword) {
		this.postgresqlPassword = postgresqlPassword;
	}

	public void setH2DataSourceClassName(String h2DataSourceClassName) {
		this.h2DataSourceClassName = h2DataSourceClassName;
	}

	public void setH2Url(String h2Url) {
		this.h2Url = h2Url;
	}

	public void setH2Username(String h2Username) {
		this.h2Username = h2Username;
	}

	public void setH2Password(String h2Password) {
		this.h2Password = h2Password;
	}
}
