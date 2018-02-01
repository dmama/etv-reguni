package ch.vd.unireg.hibernate;

import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.jdbc.LobCreationContext;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.type.Type;

import ch.vd.registre.base.utils.Assert;

public class FillHoleGenerator implements IdentifierGenerator, PersistentIdentifierGenerator, Configurable {

	private final String tableName;
	private final String seqName;
	private final int minId;
	private final int maxId;

	private String[] sqlDrop;
	private String[] sqlCreate;
	private LongSequence generator;


	public FillHoleGenerator(String name, String seqName, int minId, int maxId) {
		this.tableName = name;
		this.seqName = seqName;
		this.minId = minId;
		this.maxId = maxId;
	}

	@Override
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		generator = new LongSequence(seqName, dialect);
		sqlDrop = dialect.getDropSequenceStrings(seqName);
		sqlCreate = dialect.getCreateSequenceStrings(seqName, minId, 1);
	}

	private static final class LongSequence {

		private final String sql;

		public LongSequence(String seqName, Dialect dialect) {
			this.sql = dialect.getSequenceNextValString(seqName);
		}

		public long next(Connection connection) throws SQLException {
			try (PreparedStatement st = connection.prepareStatement(sql); ResultSet rs = st.executeQuery()) {
				if (rs.next()) {
					return rs.getLong(1);
				}
				throw new RuntimeException("La requête " + sql + " n'a renvoyé aucun résultat !");
			}
		}
	}

	@Override
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {
		return session.execute((LobCreationContext.Callback<Serializable>) this::holeNumber);
	}

	private long getFirstId(Connection session) throws SQLException {
		long firstId = generator.next(session);
		if (firstId == minId) {
			firstId = generator.next(session);
		}
		return firstId;
	}

	private Long holeNumber(Connection connection) throws SQLException {

		final long firstId = getFirstId(connection);

		// [UNIREG-726] on tient compte des no de ctbs non-migrés comme étant réservés
		final String query = "SELECT numero FROM " + tableName + " WHERE numero >= " + firstId
				+ " UNION select NO_CONTRIBUABLE from MIGREG_ERROR WHERE NO_CONTRIBUABLE >= " + firstId + " ORDER BY numero";

		long foundId = firstId;
		try (Statement stat = connection.createStatement()) {
			ResultSet rs = stat.executeQuery(query);
			try {
				while (rs.next()) {
					final int noCurrent = rs.getInt(1);
					final boolean inUse = (noCurrent == foundId);

					// Si on a un trou, le dernier ID+1 est retourné
					if (!inUse) {
						break;
					}

					// Prends le prochain ID dans la sequence
					foundId = generator.next(connection);

					// Si le numero est trop grand, on wrap
					if (foundId > maxId) {
						rs.close();
						dropAndCreateSequence(connection);
						foundId = generator.next(connection);
						rs = stat.executeQuery("SELECT numero FROM " + tableName + " WHERE numero >= " + foundId + " ORDER BY numero");
					}
				}
			}
			finally {
				rs.close();
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}

		if (foundId < minId || foundId > maxId) {
			final String message = String.format("L'Id généré [%d] n'est pas dans la plage de validité spécifiée [%d -> %d]", foundId, minId, maxId);
			Assert.fail(message);
		}

		return foundId;
	}

	@Override
	public Object generatorKey() {
		return seqName;
	}

	private void dropAndCreateSequence(Connection connection) throws SQLException {
		for (String sql : sqlDrop) {
			executeSql(connection, sql);
		}
		for (String sql : sqlCreate) {
			executeSql(connection, sql);
		}
	}

	private void executeSql(Connection connection, String sql) throws SQLException {
		try (PreparedStatement st = connection.prepareStatement(sql)) {
			// Rien a faire en cas d'erreur, true => return ResultSet, false => update ou delete
			st.execute();
		}
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return dialect.getCreateSequenceStrings(seqName, minId, 1);
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return dialect.getDropSequenceStrings(seqName);
	}

}
