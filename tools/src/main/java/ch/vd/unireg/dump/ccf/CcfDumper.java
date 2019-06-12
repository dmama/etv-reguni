package ch.vd.unireg.dump.ccf;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.pool2.BasePooledObjectFactory;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.PooledObject;
import org.apache.commons.pool2.impl.DefaultPooledObject;
import org.apache.commons.pool2.impl.GenericObjectPool;

/**
 * Permet de dumper les données telles que demandées par le CCF
 */
public class CcfDumper {

	private static final String dbDriverClassName = "oracle.jdbc.OracleDriver";

	private static final String dbUrl = "jdbc:oracle:thin:@sso0209v.etat-de-vaud.ch:1526:FUNIREGI";
	private static final String dbUser = "UNIREG";
	private static final String dbPassword = "uniregi_082";

	private static final char SEPARATOR = ';';
	private static final String NEWLINE = System.lineSeparator();

	public static void main(String[] args) throws Exception {
		Class.forName(dbDriverClassName);

		final List<Dumper> dumpers = Arrays.asList(new TiersDumper(), new ForsDumper(), new AdressesDumper(), new ImmeublesDumper());
		new CcfDumper().run(dumpers);
	}

	private void run(List<Dumper> dumpers) throws Exception {
		try (final ConnectionPool dbConnectionPool = new ConnectionPool(dbUrl, dbUser, dbPassword)) {
			for (Dumper dumper : dumpers) {
				System.out.println("Démarrage " + dumper.getClass().getSimpleName());
				final String sql = dumper.getSqlQuery();
				try (final FileWriter writer = new FileWriter(dumper.getFilename())) {
					doInNewConnection(dbConnectionPool, con -> {
						try (PreparedStatement ps = con.prepareStatement(sql)) {
							final ResultSet rs = ps.executeQuery();
							try {
								dumpHeaders(writer, rs.getMetaData());
								while (rs.next()) {
									dumpLine(writer, rs);
								}
							}
							catch (IOException e) {
								throw new RuntimeException(e);
							}
						}
						return null;
					});
				}
			}
		}
	}

	private static void dumpHeaders(Writer writer, ResultSetMetaData metadata) throws SQLException, IOException {
		final int nbCols = metadata.getColumnCount();
		for (int i = 0 ; i < nbCols ; ++ i) {
			writer.write(metadata.getColumnLabel(i + 1));
			if (i < nbCols - 1) {
				writer.write(SEPARATOR);
			}
		}
		writer.write(NEWLINE);
	}

	private static void dumpLine(Writer writer, ResultSet rs) throws SQLException, IOException {
		final ResultSetMetaData metadata = rs.getMetaData();
		final int nbCols = metadata.getColumnCount();
		for (int i = 0 ; i < nbCols ; ++ i) {
			final int dataType = metadata.getColumnType(i + 1);
			final Object data;
			switch (dataType) {
				case Types.NVARCHAR:
					data = rs.getString(i + 1);
					break;
				case Types.NUMERIC:
				case Types.BIGINT:
				case Types.BIT:
				case Types.INTEGER:
					data = rs.getLong(i + 1);
					break;
				case Types.TIMESTAMP:
					data = rs.getTimestamp(i + 1);
					break;
				default:
					throw new RuntimeException("Unknown type: " + dataType);
			}
			if (!rs.wasNull()) {
				dumpData(writer, data);
			}
			if (i < nbCols - 1) {
				writer.write(SEPARATOR);
			}
		}
		writer.write(NEWLINE);
	}

	private static void dumpData(Writer writer, Object data) throws IOException {
		final String str;
		if (data instanceof String) {
			str = StringUtils.stripEnd((String) data, null);
		}
		else if (data instanceof Number) {
			str = Long.toString(((Number) data).longValue());
		}
		else if (data instanceof Timestamp) {
			str = DateFormatUtils.format(((Timestamp) data).getTime(), "yyyy-MM-dd HH:mm:ss.SSS");
		}
		else {
			throw new RuntimeException("Unknown type : " + data.getClass());
		}

		writer.write(str);
	}

	private interface ConnectionCallback<T> {
		T doInConnection(Connection con) throws SQLException;
	}

	private <T> T doInNewConnection(ObjectPool<Connection> pool, ConnectionCallback<T> callback) throws SQLException {
		try {
			Connection con = pool.borrowObject();
			try {
				con.setAutoCommit(false);
				try {
					final T result = callback.doInConnection(con);
					con.commit();
					return result;
				}
				catch (RuntimeException | SQLException e) {
					con.rollback();
					throw e;
				}
			}
			finally {
				pool.returnObject(con);
			}
		}
		catch (RuntimeException | SQLException e) {
			throw e;
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private static final class ConnectionFactory extends BasePooledObjectFactory<Connection> {
		private final String dbUrl;
		private final String dbUser;
		private final String dbPwd;

		public ConnectionFactory(String dbUrl, String dbUser, String dbPwd) {
			this.dbUrl = dbUrl;
			this.dbUser = dbUser;
			this.dbPwd = dbPwd;
		}

		@Override
		public Connection create() throws Exception {
			return DriverManager.getConnection(dbUrl, dbUser, dbPwd);
		}

		@Override
		public PooledObject<Connection> wrap(Connection obj) {
			return new DefaultPooledObject<>(obj);
		}

		@Override
		public void destroyObject(PooledObject<Connection> p) throws Exception {
			p.getObject().close();
			super.destroyObject(p);
		}
	}

	private static final class ConnectionPool extends GenericObjectPool<Connection> implements AutoCloseable {
		private ConnectionPool(String url, String dbUser, String dbPwd) {
			super(new ConnectionFactory(url, dbUser, dbPwd));
			setMaxTotal(2);
		}
	}

	private interface Dumper {
		String getFilename();
		String getSqlQuery();
	}

	private static class TiersDumper implements Dumper {
		@Override
		public String getFilename() {
			return "tiers.csv";
		}

		@Override
		public String getSqlQuery() {
			// liste des informations de base sur les tiers
			return "SELECT NUMERO AS NUMERO_CTB, LOG_CDATE AS CREATION_DATE, LOG_CUSER AS CREATION_USER, ANNULATION_DATE, ANNULATION_USER, LOG_MDATE AS LAST_MODIFICATION_DATE, LOG_MUSER AS LAST_MODIFICATION_USER,\n" +
					"       NUMERO_INDIVIDU AS NO_RCPERS, TIERS_TYPE, DEBITEUR_INACTIF AS I107, NUMERO_COMPTE_BANCAIRE AS IBAN, TITULAIRE_COMPTE_BANCAIRE, ADRESSE_BIC_SWIFT, BLOC_REMB_AUTO\n" +
					"FROM UNIREG.TIERS ORDER BY NUMERO";
		}
	}

	private static class ForsDumper implements Dumper {
		@Override
		public String getFilename() {
			return "fors.csv";
		}

		@Override
		public String getSqlQuery() {
			return "SELECT TIERS_ID AS NUMERO_CTB, LOG_CDATE AS CREATION_DATE, LOG_CUSER AS CREATION_USER, ANNULATION_DATE, ANNULATION_USER, LOG_MDATE AS LAST_MODIFICATION_DATE, LOG_MUSER AS LAST_MODIFICATION_USER,\n" +
					"       NUMERO_OFS, TYPE_AUT_FISC AS TYPE_NUMERO_OFS, MOTIF_OUVERTURE, MOTIF_FERMETURE, FOR_TYPE, DATE_OUVERTURE, DATE_FERMETURE, MODE_IMPOSITION\n" +
					"FROM UNIREG.FOR_FISCAL ORDER BY TIERS_ID";
		}
	}

	private static class AdressesDumper implements Dumper {
		@Override
		public String getFilename() {
			return "adresses-surchargees.csv";
		}

		@Override
		public String getSqlQuery() {
			return "SELECT TIERS_ID AS NUMERO_CTB, LOG_CDATE AS CREATION_DATE, LOG_CUSER AS CREATION_USER, ANNULATION_DATE, ANNULATION_USER, LOG_MDATE AS LAST_MODIFICATION_DATE, LOG_MUSER AS LAST_MODIFICATION_USER,\n" +
					"       ADR_TYPE AS ADDRESS_TYPE, DATE_DEBUT, DATE_FIN, USAGE_TYPE AS TYPE, COMPLEMENT, NUMERO_APPARTEMENT, NUMERO_CASE_POSTALE, NUMERO_MAISON, RUE, TEXTE_CASE_POSTALE, COMPLEMENT_LOCALITE, NUMERO_OFS_PAYS, NUMERO_POSTAL_LOCALITE,\n" +
					"       NUMERO_ORDRE_POSTE, NUMERO_RUE, NPA_CASE_POSTALE\n" +
					"FROM UNIREG.ADRESSE_TIERS ORDER BY TIERS_ID";
		}
	}

	private static class ImmeublesDumper implements Dumper {
		@Override
		public String getFilename() {
			return "immeubles.csv";
		}

		@Override
		public String getSqlQuery() {
			return "SELECT CTB_ID AS NUMERO_CTB, LOG_CDATE AS CREATION_DATE, LOG_CUSER AS CREATION_USER, ANNULATION_DATE, ANNULATION_USER, LOG_MDATE AS LAST_MODIFICATION_DATE, LOG_MUSER AS LAST_MODIFICATION_USER,\n" +
					"       NOM_COMMUNE, NUMERO_IMMEUBLE, TYPE_IMMEUBLE, DATE_DEBUT, DATE_FIN\n" +
					"FROM UNIREG.IMMEUBLE ORDER BY CTB_ID";
		}
	}
}
