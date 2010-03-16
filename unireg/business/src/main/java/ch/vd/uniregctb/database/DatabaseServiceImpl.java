package ch.vd.uniregctb.database;

import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.compass.core.util.Assert;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.operation.CompositeOperation;
import org.dbunit.operation.DatabaseOperation;
import org.dbunit.operation.ManagedInsertOperation;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.dbutils.SqlFileExecutor;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;

public class DatabaseServiceImpl implements DatabaseService {

	private static final Logger LOGGER = Logger.getLogger(DatabaseServiceImpl.class);
	private static final String CORE_TRUNCATE_SQL = "/sql/core_truncate_tables.sql";

	private LocalSessionFactoryBean localSessionFactoryBean;
	private PlatformTransactionManager transactionManager;
	private DataSource dataSource;

	private final List<DatabaseListener> listeners = new ArrayList<DatabaseListener>();

	/**
	 * {@inheritDoc}
	 */
	public void ensureSequencesUpToDate(boolean updateHibernateSequence, boolean updatePMSequence, boolean updateDPISequence) {

		if (updateHibernateSequence) {
			final long maxIdAllEntities = getMaxIdOfAllHibernateEntities();
			updateSequence("hibernate_sequence", maxIdAllEntities);
		}

		if (updatePMSequence) {
			final long maxIdS_PM = Math.max(Entreprise.PM_GEN_FIRST_ID,
					getMaxIdOfTiers("'Entreprise','Etablissement','AutreCommunaute','CollectiviteAdministrative'"));
			assertMaxId("S_PM", Entreprise.PM_GEN_FIRST_ID, Entreprise.PM_GEN_LAST_ID, maxIdS_PM);
			updateSequence("S_PM", maxIdS_PM);
		}

		if (updateDPISequence) {
			final long maxIdDPI = Math.max(DebiteurPrestationImposable.FIRST_ID, getMaxIdOfTiers("'DebiteurPrestationImposable'"));
			assertMaxId("S_DPI", DebiteurPrestationImposable.FIRST_ID, DebiteurPrestationImposable.LAST_ID, maxIdDPI);
			updateSequence("S_DPI", maxIdDPI);
		}

// msi/jde : on n'a pas besoin de mettre-à-jour la séquence parce qu'elle est associée avec le FillHoleGenerator qui fonctionne différemment
//	         (en particulier elle est ré-initialisée dès qu'elle touche la limite supérieure)
//        final long maxIdS_CTB = getMaxIdOfTiers("'Habitant','NonHabitant','MenageCommun'");
//        Assert.isTrue(Contribuable.CTB_GEN_FIRST_ID <= maxIdS_CTB && maxIdS_CTB < Contribuable.CTB_GEN_LAST_ID);
//        updateSequence("S_CTB", maxIdS_CTB);
	}

	private static void assertMaxId(String sequence, long min, long max, long id) {
		Assert.isTrue(id < max, String.format("Séquence %s: l'ID maximum trouvé %d est en dehors de la plage de validité %d -> %d",
				sequence, id, min, max));
	}

	private void updateSequence(String sequenceName, long maxId) {
		// Incrémente artificiellement la séquence jusqu'à ce que maxId soit atteint

		Dialect dialect = Dialect.getDialect(localSessionFactoryBean.getConfiguration().getProperties());
		String sql = dialect.getSequenceNextValString(sequenceName);

        final JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setIgnoreWarnings(false);

        long startId = template.queryForLong(sql);

		if (dialect instanceof Oracle8iDialect) {
			if (startId < maxId) {
				long inc = maxId - startId + 1;

				// on augmente l'incrément de la valeur qui va bien
				String alter = "alter sequence "+sequenceName+" increment by " + inc;
				template.execute(alter);

				// on demande le prochain id pour mettre-à-jour la séquence
				String select = "select "+sequenceName+".nextval from dual";
				long id = template.queryForLong(select);
				Assert.isTrue(id > maxId);

				// on reset l'incrément à 1
				alter = "alter sequence "+sequenceName+" increment by 1";
				template.execute(alter);
			}
		}
		else {
	        long id = -1;
			do {
				id = template.queryForLong(sql);
			} while (id < maxId);
		}

		long newId = template.queryForLong(sql);
		Assert.isTrue(newId > maxId);
	}

	/**
	 * Retourne l'id max des entités hibernate actuellement présentes en base (à l'exception des entités de la table TIERS),
	 */
	@SuppressWarnings("unchecked")
	private long getMaxIdOfAllHibernateEntities() {

		long max = -1;

		final JdbcTemplate template = new JdbcTemplate(dataSource);
		template.setIgnoreWarnings(false);

		final Iterator<Table> tables = localSessionFactoryBean.getConfiguration().getTableMappings();
		while (tables.hasNext()) {

			final Table table = tables.next();
			if (!table.isPhysicalTable()) {
				continue;
			}
			if (table.getName().equals("TIERS")) {
				// la table tiers n'utilise pas la sequence hibernate
				continue;
			}
			if (table.getName().equals("PARAMETRE")) {
				// la table parametre n'utilise pas d'id numérique
				continue;
			}

			final Column identifier = (Column) table.getIdentifierValue().getColumnIterator().next();
			final String sql = "select max(" + identifier.getName() + ") from " + table.getName();

			long id = template.queryForLong(sql);
			max = (id > max ? id : max);
		}

		return max;
	}

	private long getMaxIdOfTiers(String listeEntite) {

		long max = -1;

		final JdbcTemplate template = new JdbcTemplate(dataSource);
		template.setIgnoreWarnings(false);

		final String sql = "select max(NUMERO) from TIERS where TIERS_TYPE in (" + listeEntite + ")";

		long id = template.queryForLong(sql);
		max = (id > max ? id : max);

		return max;
	}

	/**
	 * {@inheritDoc}
	 */
	public void truncateDatabase() throws Exception {
		LOGGER.debug("Truncating database");
		try {
			onTruncateDatabase();
			SqlFileExecutor.execute(transactionManager, dataSource, CORE_TRUNCATE_SQL);
		}
		catch (Exception e) {
			LOGGER.error("Exception lors du truncate de la base.", e);

			/* Debug code pour aider à comprendre des problèmes de tables qui disparaissent dans les test unitaires sous Hudson */
			final String[] names = getTableNamesFromDatabase();
			if (names == null) {
				LOGGER.error("Il n'y a aucune table présente dans la base de données.");
			}
			else {
				LOGGER.error("Il y a réellement " + names.length + " tables présentes dans la base de données:");
				for (String n : names) {
					LOGGER.error(" - " + n);
				}
			}

			throw e;
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public String[] getTableNamesFromDatabase() {

		Dialect dialect = Dialect.getDialect(localSessionFactoryBean.getConfiguration().getProperties());

		if (dialect instanceof Oracle8iDialect) {
			final String sql = "SELECT table_name FROM user_tables";

			final JdbcTemplate template = new JdbcTemplate(dataSource);
			template.setIgnoreWarnings(false);

			TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
			List<ListOrderedMap> rs = (List<ListOrderedMap>) tmpl.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					return template.queryForList(sql);
				}
			});

			List<String> list = new ArrayList<String>();
			for (ListOrderedMap map : rs) {
				String name = (String) map.getValue(0);
				list.add(name);
			}
			return list.toArray(new String[] {});
		}

		return null;
	}

	/**
	 * {@inheritDoc}
	 */
	@SuppressWarnings("unchecked")
	public String[] getTableNamesFromHibernate(boolean reverse) {
		ArrayList<String> t = new ArrayList<String>();
		Iterator<Table> tables = localSessionFactoryBean.getConfiguration().getTableMappings();
		while (tables.hasNext()) {
			Table table = tables.next();
			if (table.isPhysicalTable()) {
				addTableName(t, table);
			}
		}
		if (reverse) {
			Collections.reverse(t);
		}
		return t.toArray(new String[t.size()]);
	}

	/**
	 *
	 * @param tables
	 * @param table
	 */
	@SuppressWarnings("unchecked")
	private void addTableName(ArrayList<String> tables, Table table) {
		if (tables.contains(table.getName())) {
			return;
		}
		Iterator<Table> ts = localSessionFactoryBean.getConfiguration().getTableMappings();
		while (ts.hasNext()) {
			Table t = ts.next();
			if (t.equals(table)) {
				continue;
			}
			Iterator<ForeignKey> relationships = t.getForeignKeyIterator();
			while (relationships.hasNext()) {
				ForeignKey fk = relationships.next();
				if (fk.getReferencedTable().equals(table)) {
					addTableName(tables, fk.getTable());
				}
			}
		}
		tables.add(table.getName());
	}

	protected static DatabaseConnection createNewDatabaseConnection(Connection connection) {
		final DatabaseConnection dbConnection = new DatabaseConnection(connection);
		final DatabaseConfig config = dbConnection.getConfig();
		config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new OracleDataTypeFactory());
		return dbConnection;
}

	/**
	 * {@inheritDoc}
	 */
	public void dumpToDbunitFile(OutputStream outputStream) throws Exception {

		Connection con = DataSourceUtils.getConnection(dataSource);
		try {
			DatabaseConnection connection = createNewDatabaseConnection(con);
			QueryDataSet dataSet = new QueryDataSet(connection);

			// Dump dans l'ordre inverse de manière à ne pas péter les foreign keys lors du ré-import.
			final String[] tables = getTableNamesFromHibernate(true);
			for (String table : tables) {

				if ("AUDIT_LOG".equals(table)) {
					/*
					 * on ne dump pas la table d'audit log, de manière à ne pas l'écraser lors du rechargement et à garder l'historique réel
					 * des opérations sur la base
					 */
				}
				else if ("DOC_INDEX".equals(table)) {
					/*
					 * on ne dump pas la table des indexes de documents car les documents eux-mêmes restent sur le disque du serveur
					 */
				}
				else {
					dataSet.addTable(table);
				}
			}

			// XML file into which data needs to be extracted
			LOGGER.info("Début de l'export de la base de données.");
			XmlDataSet.write(dataSet, outputStream);
			LOGGER.info("La base de données à été exportée.");
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public int dumpTiersListToDbunitFile(List<Long> tiersList, OutputStream outputStream) throws Exception {
		
		final Connection con = DataSourceUtils.getConnection(dataSource);
		try {
			final DatabaseConnection connection = createNewDatabaseConnection(con);
			final QueryDataSet dataSet = new QueryDataSet(connection);
			
			final String tiersIdSeparated = generateSeparatedId(tiersList);
			
			// RAPPORT_ENTRE_TIERS
			final String RAPPORTS_TABLE_NAME = "RAPPORT_ENTRE_TIERS";
			final String RAPPORTS_QUERY = String.format("select * from %1$s where TIERS_OBJET_ID in (%2$s) or TIERS_SUJET_ID in (%2$s)" +
					" union select * from %1$s where TIERS_OBJET_ID in" +
					" (select TIERS_OBJET_ID from %1$s where TIERS_OBJET_ID in (%2$s) or TIERS_SUJET_ID in (%2$s))", RAPPORTS_TABLE_NAME, tiersIdSeparated);
			
			// TIERS
			final String TIERS_TABLE_NAME = "TIERS";
			final String TIERS_QUERY = String.format("select * from %1$s where NUMERO in (%3$s)"
					+ " union select * from %1$s where NUMERO in (%4$s)"
					+ " union select * from %1$s where NUMERO in (%2$s)", 
					TIERS_TABLE_NAME, tiersIdSeparated,
					RAPPORTS_QUERY.replace("*", "TIERS_SUJET_ID"), RAPPORTS_QUERY.replace("*", "TIERS_OBJET_ID"));
			
			dataSet.addTable(TIERS_TABLE_NAME, TIERS_QUERY);
			dataSet.addTable(RAPPORTS_TABLE_NAME, RAPPORTS_QUERY);
			
			// FOR_FISCAL
			final String FORS_TABLE_NAME = "FOR_FISCAL";
			final String FORS_QUERY = String.format("select * from %1$s where TIERS_ID in (%2$s)", FORS_TABLE_NAME, TIERS_QUERY.replace("*", "NUMERO"));
			dataSet.addTable(FORS_TABLE_NAME, FORS_QUERY);
			
			// ADRESSE_TIERS
			final String ADRESSES_TABLE_NAME = "ADRESSE_TIERS";
			final String ADRESSES_QUERY = String.format("select * from %1$s where TIERS_ID in (%2$s)", ADRESSES_TABLE_NAME, TIERS_QUERY.replace("*", "NUMERO"));
			dataSet.addTable(ADRESSES_TABLE_NAME, ADRESSES_QUERY);

			// SITUATION_FAMILLE
			final String SF_TABLE_NAME = "SITUATION_FAMILLE";
			final String SF_QUERY = String.format("select * from %1$s where CTB_ID in (%2$s)", SF_TABLE_NAME, TIERS_QUERY.replace("*", "NUMERO"));
			dataSet.addTable(SF_TABLE_NAME, SF_QUERY);
			
			// XML file into which data needs to be extracted
			LOGGER.info("Début de l'export des tiers.");
			XmlDataSet.write(dataSet, outputStream);
			LOGGER.info("La liste de tiers à été exportée.");
			
			return dataSet.getTable(TIERS_TABLE_NAME).getRowCount();
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}
	}
	
	private String generateSeparatedId(List<Long> idList) {
		String separatedIds ="";
		for (Long id : idList) {
			separatedIds += ("".endsWith(separatedIds) ? "" : ",") + id.toString();
		}
		return separatedIds;
	}

	/**
	 * {@inheritDoc}
	 */
	public void loadFromDbunitFile(InputStream inputStream, StatusManager status) throws Exception {

		Connection con = DataSourceUtils.getConnection(dataSource);
		try {
			DatabaseConnection connection = createNewDatabaseConnection(con);

			XmlDataSet dataSet = new XmlDataSet(inputStream);
			LOGGER.info("Début de l'import de la base de données.");
			onTruncateDatabase();
			CompositeOperation operation = new CompositeOperation(DatabaseOperation.DELETE_ALL, new ManagedInsertOperation(status));
			operation.execute(connection, dataSet);
			onLoadDatabase();
			LOGGER.info("La base de données à été importée.");
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}
	}

	public void setLocalSessionFactoryBean(LocalSessionFactoryBean localSessionFactoryBean) {
		this.localSessionFactoryBean = localSessionFactoryBean;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	private void onTruncateDatabase() {
		for (DatabaseListener l : listeners) {
			try {
				l.onTruncateDatabase();
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	private void onLoadDatabase() {
		for (DatabaseListener l : listeners) {
			try {
				l.onLoadDatabase();
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	public void onTiersChange(long id) {
		for (DatabaseListener l : listeners) {
			try {
				l.onTiersChange(id);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	public void onDroitAccessChange(long ppId) {
		for (DatabaseListener l : listeners) {
			try {
				l.onDroitAccessChange(ppId);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	public void register(DatabaseListener listener) {
		listeners.add(listener);
	}

}
