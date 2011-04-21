package ch.vd.uniregctb.database;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.map.ListOrderedMap;
import org.apache.log4j.Logger;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.DataSetException;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.ITable;
import org.dbunit.dataset.ITableIterator;
import org.dbunit.dataset.ManagedDataSet;
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
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.dbutils.SqlFileExecutor;
import ch.vd.uniregctb.tiers.DebiteurPrestationImposable;
import ch.vd.uniregctb.tiers.Entreprise;

public class DatabaseServiceImpl implements DatabaseService {

	private static final Logger LOGGER = Logger.getLogger(DatabaseServiceImpl.class);
	private static final String CORE_TRUNCATE_SQL = "/sql/core_truncate_tables.sql";

	private LocalSessionFactoryBean localSessionFactoryBean;
	private PlatformTransactionManager transactionManager;
	private DataSource dataSource;
	private DataEventService dataEventService;

	private static final int DEFAULT_BATCH_SIZE = 500;

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
				String alter = "alter sequence " + sequenceName + " increment by " + inc;
				template.execute(alter);

				// on demande le prochain id pour mettre-à-jour la séquence
				String select = "select " + sequenceName + ".nextval from dual";
				long id = template.queryForLong(select);
				Assert.isTrue(id > maxId);

				// on reset l'incrément à 1
				alter = "alter sequence " + sequenceName + " increment by 1";
				template.execute(alter);
			}
		}
		else {
			long id;
			do {
				id = template.queryForLong(sql);
			} while (id < maxId);
		}

		long newId = template.queryForLong(sql);
		Assert.isTrue(newId > maxId);
	}

	/**
	 * @return l'id max des entités hibernate actuellement présentes en base (à l'exception des entités de la table TIERS),
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
			dataEventService.onTruncateDatabase();
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
			return list.toArray(new String[list.size()]);
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
	public int dumpTiersListToDbunitFile(List<Long> tiersIds, final DumpParts parts, OutputStream outputStream, StatusManager status) throws Exception {

		status.setMessage("Analyse des données...");
		final Set<Long> allTiersIds = new HashSet<Long>();
		final Set<Long> allRETIds = new HashSet<Long>();
		determineAllRelatedIds(tiersIds, allTiersIds, allRETIds, parts);

		final Connection con = DataSourceUtils.getConnection(dataSource);
		try {
			final DatabaseConnection connection = createNewDatabaseConnection(con);

			// Les données indépendante des tiers
			final List<ITable> otherTables;
			if (parts.declarations) {
				final QueryDataSet q = new QueryDataSet(connection);
				q.addTable("PERIODE_FISCALE");
				q.addTable("PARAMETRE_PERIODE_FISCALE");
				q.addTable("PARAMETRE");
				q.addTable("MODELE_DOCUMENT");
				q.addTable("MODELE_FEUILLE_DOC");

				otherTables = new ArrayList<ITable>();

				final ITableIterator iter = q.iterator();
				while (iter.next()) {
					otherTables.add(iter.getTable());
				}
			}
			else {
				otherTables = Collections.emptyList();
			}

			if (status.interrupted()) {
				return -1;
			}

			// Les données des tiers et autres données appartenant aux tiers
			final List<ITable> tiersTables = queryDataSet(allTiersIds, connection, "Récupération des tiers", status, new QueryDataSetCallback() {
				public QueryDataSet execute(Collection<Long> ids, DatabaseConnection connection) throws SQLException {
					return queryTiersData(ids, parts, connection);
				}
			});

			if (status.interrupted()) {
				return -1;
			}

			final List<ITable> retTables;
			if (parts.rapportsEntreTiers && !allRETIds.isEmpty()) {
				// Les rapports-entre-tiers
				retTables = queryDataSet(allRETIds, connection, "Récupération des rapports-entre-tiers", status, new QueryDataSetCallback() {
					public QueryDataSet execute(Collection<Long> ids, DatabaseConnection connection) throws SQLException {
						return queryRapportEntreTiersData(ids, connection);
					}
				});
			}
			else {
				retTables = Collections.emptyList();
			}

			final List<ITable> allTables = new ArrayList<ITable>(tiersTables.size() + retTables.size());
			allTables.addAll(otherTables);
			allTables.addAll(tiersTables);
			allTables.addAll(retTables);

			final IDataSet dataSet = new CompositeDataSet(allTables.toArray(new ITable[allTables.size()]));

			if (status.interrupted()) {
				return -1;
			}

			// XML file into which data needs to be extracted
			status.setMessage("Export des données en fichiers XML...");
			XmlDataSet.write(new ManagedDataSet(dataSet, status), outputStream);
			status.setMessage("Terminé.");

			return dataSet.getTable("TIERS").getRowCount();
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			throw e;
		}
		finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}
	}

	private static interface QueryDataSetCallback {
		QueryDataSet execute(Collection<Long> ids, DatabaseConnection connection) throws SQLException;
	}

	private List<ITable> queryDataSet(Collection<Long> ids, DatabaseConnection connection, String message, StatusManager status, QueryDataSetCallback callback) throws SQLException, DataSetException {

		final List<ITable> tables = new ArrayList<ITable>();

		final List<Long> idsList = new ArrayList<Long>(ids);
		Collections.sort(idsList);

		if (idsList.size() <= DEFAULT_BATCH_SIZE) {
			status.setMessage(message + "...");
			final QueryDataSet q = callback.execute(idsList, connection);

			final ITableIterator iter = q.iterator();
			while (iter.next() && !status.interrupted()) {
				tables.add(iter.getTable());
			}
		}
		else {
			// découpe par batchs
			final List<List<Long>> batches = split(idsList, DEFAULT_BATCH_SIZE);
			final int count = batches.size();

			for (int i = 0; i < count && !status.interrupted(); i++) {
				status.setMessage(message + " (lot " + i + "/" + count + ")...");
				final List<Long> batch = batches.get(i);
				final QueryDataSet q = callback.execute(batch, connection);

				final ITableIterator iter = q.iterator();
				while (iter.next() && !status.interrupted()) {
					tables.add(iter.getTable());
				}
			}
		}

		return tables;
	}

	/**
	 * Détermine l'ensemble des tiers en relation avec les tiers spécifiés.
	 *
	 * @param inputTiersIds   les ids des tiers d'entrée.
	 * @param relatedTiersIds les ids de l'ensemble des tiers en relation avec les tiers d'entrée.
	 * @param relatedRETIds   les ids des rapports-entre-tiers correspondant aux tiers trouvés.
	 * @param parts           les parties à tenir compte
	 * @throws SQLException en cas de problème SQL
	 */
	private void determineAllRelatedIds(List<Long> inputTiersIds, Set<Long> relatedTiersIds, final Set<Long> relatedRETIds, DumpParts parts) throws SQLException {

		if (parts.rapportsEntreTiers) {

			final String sql =
					"select ID, TIERS_OBJET_ID, TIERS_SUJET_ID, TIERS_TUTEUR_ID from RAPPORT_ENTRE_TIERS where RAPPORT_ENTRE_TIERS_TYPE != 'RapportPrestationImposable' and TIERS_OBJET_ID in (:ids)"
							+
							" union select ID, TIERS_OBJET_ID, TIERS_SUJET_ID, TIERS_TUTEUR_ID from RAPPORT_ENTRE_TIERS where RAPPORT_ENTRE_TIERS_TYPE != 'RapportPrestationImposable' and TIERS_SUJET_ID in (:ids)";

			NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

			Set<Long> newIds = new HashSet<Long>(inputTiersIds);

			// On recherche tous les ids en relation avec ceux spécifié. Pour cela il peut être nécessaire de faire 'n' passes en fonction de la profondeur des relations.
			for (; ;) {

				if (newIds.size() <= DEFAULT_BATCH_SIZE) {

					final List<Long> foundIds = new ArrayList<Long>();
					template.query(sql, Collections.singletonMap("ids", newIds), new RowCallbackHandler() {
						public void processRow(ResultSet rs) throws SQLException {
							relatedRETIds.add(rs.getLong(1)); // ID
							foundIds.add(rs.getLong(2)); // TIERS_OBJET_ID
							foundIds.add(rs.getLong(3)); // TIERS_SUJET_ID
							final long tuteurId = rs.getLong(4); // TIERS_TUTEUR_ID
							if (tuteurId > 0) {
								foundIds.add(tuteurId);
							}
						}
					});

					if (relatedTiersIds.containsAll(foundIds)) {
						// tous les ids ont été trouvés -> c'est terminé
						break;
					}

					// extrait la liste des ids nouvellement trouvés
					newIds = new HashSet<Long>();
					for (Long id : foundIds) {
						if (!relatedTiersIds.contains(id)) {
							newIds.add(id);
						}
					}

					// on ajoute les nouveaux ids
					relatedTiersIds.addAll(newIds);
				}
				else {
					// on a trop d'ids pour les traiter d'une seule requête -> découpe par batchs
					final List<List<Long>> batches = split(new ArrayList<Long>(newIds), DEFAULT_BATCH_SIZE);
					for (List<Long> batch : batches) {
						determineAllRelatedIds(batch, relatedTiersIds, relatedRETIds, parts); // appel récursif
					}
					break;
				}
			}
		}
		
		// les ids d'entrées doivent aussi être utilisés
		relatedTiersIds.addAll(inputTiersIds);

		if (parts.declarations) {
			// On ajoute toutes les collectivités administatives référencées par les déclarations
			final String sql = "select distinct(RETOUR_COLL_ADMIN_ID) from DECLARATION where RETOUR_COLL_ADMIN_ID is not null";

			NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
			@SuppressWarnings({"unchecked"})
			List<Long> ids = template.query(sql, new HashMap(), new RowMapper() {
				public Object mapRow(ResultSet rs, int rowNum) throws SQLException {
					return rs.getLong(1);
				}
			});

			relatedTiersIds.addAll(ids);
		}
	}

	private static <T> List<List<T>> split(List<T> list, int size) {

		List<List<T>> r = new ArrayList<List<T>>();

		for (int i = 0; i < list.size(); i += size) {
			int j = (i + size <= list.size() ? i + size : list.size());
			r.add(list.subList(i, j));
		}

		return r;
	}

	private static QueryDataSet queryTiersData(Collection<Long> ids, DumpParts parts, DatabaseConnection connection) throws SQLException {
		final QueryDataSet dataSet = new QueryDataSet(connection);

		final String idsList = generateSeparatedId(ids);

		// TIERS
		final String TIERS_TABLE_NAME = "TIERS";
		final String TIERS_QUERY = String.format("select * from %1$s where NUMERO in (%2$s)", TIERS_TABLE_NAME, idsList);
		dataSet.addTable(TIERS_TABLE_NAME, TIERS_QUERY);

		// FOR_FISCAL
		final String FORS_TABLE_NAME = "FOR_FISCAL";
		final String FORS_QUERY = String.format("select * from %1$s where TIERS_ID in (%2$s)", FORS_TABLE_NAME, idsList);
		dataSet.addTable(FORS_TABLE_NAME, FORS_QUERY);

		// ADRESSE_TIERS
		final String ADRESSES_TABLE_NAME = "ADRESSE_TIERS";
		final String ADRESSES_QUERY = String.format("select * from %1$s where TIERS_ID in (%2$s)", ADRESSES_TABLE_NAME, idsList);
		dataSet.addTable(ADRESSES_TABLE_NAME, ADRESSES_QUERY);

		if (parts.sitFamille) {
			// SITUATION_FAMILLE
			final String SF_TABLE_NAME = "SITUATION_FAMILLE";
			final String SF_QUERY = String.format("select * from %1$s where CTB_ID in (%2$s)", SF_TABLE_NAME, idsList);
			dataSet.addTable(SF_TABLE_NAME, SF_QUERY);
		}

		if (parts.declarations) {

			final String query1 = String.format("select * from DECLARATION where TIERS_ID in (%s)", idsList);
			dataSet.addTable("DECLARATION", query1);

			final String query2 = String.format("select * from ETAT_DECLARATION where DECLARATION_ID in (select ID from DECLARATION where TIERS_ID in (%s))", idsList);
			dataSet.addTable("ETAT_DECLARATION", query2);

			final String query3 = String.format("select * from DELAI_DECLARATION where DECLARATION_ID in (select ID from DECLARATION where TIERS_ID in (%s))", idsList);
			dataSet.addTable("DELAI_DECLARATION", query3);
		}

		return dataSet;
	}

	private static QueryDataSet queryRapportEntreTiersData(Collection<Long> ids, DatabaseConnection connection) throws SQLException {
		final QueryDataSet dataSet = new QueryDataSet(connection);

		final String idsList = generateSeparatedId(ids);

		// RAPPORT_ENTRE_TIERS
		final String RAPPORTS_TABLE_NAME = "RAPPORT_ENTRE_TIERS";
		final String RAPPORTS_QUERY = String.format("select * from %1$s where RAPPORT_ENTRE_TIERS_TYPE != 'RapportPrestationImposable' and ID in (%2$s)", RAPPORTS_TABLE_NAME, idsList);
		dataSet.addTable(RAPPORTS_TABLE_NAME, RAPPORTS_QUERY);

		return dataSet;
	}

	private static String generateSeparatedId(Collection<Long> ids) {
		StringBuilder b = new StringBuilder();
		for (Long id : ids) {
			b.append(id.toString()).append(",");
		}
		final String s = b.toString();
		return s.substring(0, s.length() - 1); // supprime la dernière virgule
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
			dataEventService.onTruncateDatabase();
			CompositeOperation operation = new CompositeOperation(DatabaseOperation.DELETE_ALL, new ManagedInsertOperation(status));
			operation.execute(connection, dataSet);
			dataEventService.onLoadDatabase();
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

	@SuppressWarnings({"UnusedDeclaration"})
	public void setLocalSessionFactoryBean(LocalSessionFactoryBean localSessionFactoryBean) {
		this.localSessionFactoryBean = localSessionFactoryBean;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}
}
