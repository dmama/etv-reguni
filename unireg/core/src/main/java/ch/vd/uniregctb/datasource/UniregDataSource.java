package ch.vd.uniregctb.datasource;

import java.sql.SQLException;

import javax.sql.DataSource;

import oracle.jdbc.OracleDriver;

import org.apache.commons.dbcp.BasicDataSource;

/**
 * Datasource à utiliser pour la gestion des NVARCHAR (unicode)
 */
public class UniregDataSource extends BasicDataSource {

	@Override
	protected synchronized DataSource createDataSource() throws SQLException {
		addConnectionProperty(OracleDriver.defaultnchar_string, "true");
		return super.createDataSource();
	}
}
