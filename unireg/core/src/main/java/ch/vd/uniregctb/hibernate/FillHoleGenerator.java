package ch.vd.uniregctb.hibernate;

import java.io.Serializable;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.dialect.Dialect;
import org.hibernate.engine.SessionImplementor;
import org.hibernate.exception.JDBCExceptionHelper;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.id.PersistentIdentifierGenerator;
import org.hibernate.id.enhanced.SequenceStyleGenerator;
import org.hibernate.type.Type;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.tiers.Contribuable;

public class FillHoleGenerator implements IdentifierGenerator, PersistentIdentifierGenerator, Configurable {

	//private static final Logger LOGGER = Logger.getLogger(FillHoleGenerator.class);

	private final String tableName;
	private final String seqName;
	private final int minId;
	private final int maxId;

	private String[] sqlDrop;
	private String[] sqlCreate;
	private SequenceStyleGenerator generator;


	public FillHoleGenerator(String name, String seqName, int minId, int maxId) {
		this.tableName = name;
		this.seqName = seqName;
		this.minId = minId;
		this.maxId = maxId;
	}

	@Override
	public void configure(Type type, Properties params, Dialect dialect) throws MappingException {
		Properties properties = new Properties();
		properties.putAll(params);
		properties.put("sequence_name", seqName);
		properties.put("initial_value", String.valueOf(minId));

		generator = new SequenceStyleGenerator();
		generator.configure(type, properties, dialect);

		sqlDrop = generator.sqlDropStrings(dialect);
		sqlCreate = generator.sqlCreateStrings(dialect);
	}

	@Override
	public Serializable generate(SessionImplementor session, Object object) throws HibernateException {

		//return randomNumber(session, object);
		return holeNumber(session, object);
	}

//	private Long randomNumber(SessionImplementor session, Object object) {
//		Integer i = 10000000 + new Random().nextInt(80000000);
//		return i.longValue();
//	}

	private Long holeNumber(SessionImplementor session, Object object) {

		Long firstId = (Long) generator.generate(session, object);
		if (firstId == minId) {
			firstId = (Long) generator.generate(session, object);
		}

		Long foundId = null;
		Statement stat = null;
		ResultSet rs = null;
		try {
			Connection con = session.connection();
			stat = con.createStatement();
			// [UNIREG-726] on tient compte des no de ctbs non-migrés comme étant réservés
			final String query = "SELECT numero FROM " + tableName + " WHERE numero >= " + firstId
					+ " UNION select NO_CONTRIBUABLE from MIGREG_ERROR WHERE NO_CONTRIBUABLE >= " + firstId + " ORDER BY numero";
			rs = stat.executeQuery(query);

			foundId = firstId;
			while (rs.next()) {
				final int noCurrent = rs.getInt(1);
				final boolean inUse = (noCurrent == foundId);

				// Si on a un trou, le dernier ID+1 est retourné
				if (!inUse) {
					break;
				}

				// Prends le prochain ID dans la sequence
				foundId = (Long) generator.generate(session, object);

				// Si le numero est trop grand, on wrap
				if (foundId > maxId) {
					rs.close();
					dropAndCreateSequence(session);
					foundId = (Long) generator.generate(session, object);
					rs = stat.executeQuery("SELECT numero FROM " + tableName + " WHERE numero >= " + foundId + " ORDER BY numero");
				}
			}
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			try {
				if (rs != null) {
					rs.close();
				}
				if (stat != null) {
					stat.close();
				}
			}
			catch (SQLException e) {
				// sous-exception ignorée
			}
		}

		Assert.isTrue(foundId != null);
		final int l = foundId.intValue();
		if (l < minId || l > maxId) {
			final String message = String.format(
					"L'Id généré [%d] pour l'object de type %s n'est pas dans la plage de validité spécifiée [%d -> %d]", l, object
							.getClass().getSimpleName(), minId, maxId);
			Assert.fail(message);
		}

		return (long) l;
	}

	@Override
	public Object generatorKey() {
		return seqName;
	}

	private void dropAndCreateSequence(SessionImplementor session) {
		for (String sql : sqlDrop) {
			executeSql(session, sql);
		}
		for (String sql : sqlCreate) {
			executeSql(session, sql);
		}
	}

	private void executeSql(SessionImplementor session, String sql) {
		try {

			PreparedStatement st = session.getBatcher().prepareSelectStatement(sql);
			try {
				// Rien a faire en cas d'erreur, true => return ResultSet, false => update ou delete
				st.execute();
			}
			finally {
				session.getBatcher().closeStatement(st);
			}
		}
		catch (SQLException sqle) {
			throw JDBCExceptionHelper.convert(
					session.getFactory().getSQLExceptionConverter(),
					sqle,
					"could not get next sequence value",
					sql
				);
		}
	}

	@Override
	public String[] sqlCreateStrings(Dialect dialect) throws HibernateException {
		return generator.sqlCreateStrings(dialect);
	}

	@Override
	public String[] sqlDropStrings(Dialect dialect) throws HibernateException {
		return generator.sqlDropStrings(dialect);
	}
	
	/**
	 * Un petit programme qui retrouve le premier numero de tiers disponible pour un ctb en prod.
	 * 
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		
		DriverManagerDataSource ds = new DriverManagerDataSource();
		ds.setDriverClassName("oracle.jdbc.driver.OracleDriver");
		ds.setUrl("jdbc:oracle:thin:@sso0211p.etat-de-vaud.ch:1525:FURP");
		ds.setUsername("dsi_read");
		ds.setPassword("dsi_read_1014");
		JdbcTemplate jdbcTemplate = new JdbcTemplate(ds);
		jdbcTemplate.setFetchSize(1000);
		jdbcTemplate.setMaxRows(1000);
		
		int indexDebut = Contribuable.CTB_GEN_FIRST_ID + 1;
		int noTiers = indexDebut;
		
		while ( noTiers < Contribuable.CTB_GEN_LAST_ID ) {
		
			List res = jdbcTemplate.queryForList(
					" select numero n" +
					" from unireg.TIERS " +
					" where numero >= " + indexDebut +
					" union " +
					" select no_contribuable n " +
					" from unireg.MIGREG_ERROR " +
					" where no_contribuable >= " + indexDebut +
					" order by n");
			
			
			for (Object row : res) {
				if (((BigDecimal)((Map)row).get("n")).intValue() > noTiers) {
					System.out.println("Prochain numero disponible : " + noTiers);
					System.exit(0);
				}
				noTiers++;
			}
			indexDebut = noTiers;
			
		}
		System.out.println("Aucun numero disponible...");
		System.exit(1);
	}
	
	

}
