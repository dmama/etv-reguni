package ch.vd.uniregctb.datasource;

import java.sql.SQLException;
import java.util.Properties;

import oracle.jdbc.OracleDriver;
import oracle.jdbc.xa.client.OracleXADataSource;
import org.apache.commons.lang3.StringUtils;
import org.tranql.connector.oracle.XAMCF;

/**
 * Classe à utiliser pour les managed connection factories
 */
public class UniregXAMCF extends XAMCF {

	private static final long serialVersionUID = -8621566872461190601L;

	public UniregXAMCF() throws SQLException {
		super();
		final OracleXADataSource ds = xaDataSource;

		final Properties props = new Properties();
		props.setProperty(OracleDriver.defaultncharprop_string, "true");
		ds.setConnectionProperties(props);
	}

	@Override
	public void setServiceName(String serviceName) {
		super.setServiceName(StringUtils.trimToNull(serviceName));
	}

	@Override
	public void setDatabaseName(String dbname) {
		super.setDatabaseName(StringUtils.trimToNull(dbname));
	}
}
