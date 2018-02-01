package ch.vd.unireg.datasource;

import javax.sql.DataSource;
import java.sql.SQLException;

import oracle.jdbc.OracleDriver;
import org.apache.commons.dbcp2.BasicDataSource;

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
