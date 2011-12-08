package ch.vd.uniregctb.datasource;

import java.sql.SQLException;
import java.util.Properties;

import oracle.jdbc.OracleDriver;
import oracle.jdbc.xa.client.OracleXADataSource;

import org.tranql.connector.oracle.XAMCF;

/**
 * Classe à utiliser pour les managed connection factories
 */
public class UniregXAMCF extends XAMCF {

	private static final long serialVersionUID = 1662465627225728830L;

	public UniregXAMCF() throws SQLException {
		super();
		final OracleXADataSource ds = (OracleXADataSource) xaDataSource;

		final Properties props = new Properties();
		props.setProperty(OracleDriver.defaultncharprop_string, "true");
		ds.setConnectionProperties(props);
	}
}
