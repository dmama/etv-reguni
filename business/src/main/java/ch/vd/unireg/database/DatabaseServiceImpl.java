package ch.vd.unireg.database;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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
import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.Oracle8iDialect;
import org.hibernate.mapping.Column;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.data.FiscalDataEventNotifier;
import ch.vd.unireg.dbutils.SqlFileExecutor;
import ch.vd.unireg.tiers.AutreCommunaute;
import ch.vd.unireg.tiers.DebiteurPrestationImposable;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.Etablissement;

public class DatabaseServiceImpl implements DatabaseService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseServiceImpl.class);
	private static final String CORE_TRUNCATE_SQL = "/sql/core_truncate_tables.sql";

	private Dialect hibernateDialect;
	private PlatformTransactionManager transactionManager;
	private DataSource dataSource;
	private FiscalDataEventNotifier fiscalDataEventNotifier;

	private static final int DEFAULT_BATCH_SIZE = 500;

	@SuppressWarnings("UnusedReturnValue")
	private <T> T doInNewTransaction(boolean readonly, TransactionCallback<T> action) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(readonly);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(action);
	}

	private void sendTruncateDatabaseEvent() {
		doInNewTransaction(false, status -> {
			fiscalDataEventNotifier.notifyTruncateDatabase();
			return null;
		});
	}

	private void sendLoadDatabaseEvent() {
		doInNewTransaction(false, status -> {
			fiscalDataEventNotifier.notifyLoadDatabase();
			return null;
		});
	}

	@Override
	public void ensureSequencesUpToDate(boolean updateHibernateSequence, boolean updateCAACSequence, boolean updateDPISequence, boolean updatePMSequence, boolean updateETBSequence) {

		if (updateHibernateSequence) {
			final long maxIdAllEntities = getMaxIdOfAllHibernateEntities();
			updateSequence("hibernate_sequence", maxIdAllEntities);
		}

		if (updateCAACSequence) {
			final long maxIdS_CAAC = Math.max(AutreCommunaute.CAAC_GEN_FIRST_ID, getMaxIdOfTiers("'AutreCommunaute','CollectiviteAdministrative'"));
			assertMaxId("S_CAAC", AutreCommunaute.CAAC_GEN_FIRST_ID, AutreCommunaute.CAAC_GEN_LAST_ID, maxIdS_CAAC);
			updateSequence("S_CAAC", maxIdS_CAAC);
		}

		if (updatePMSequence) {
			final long maxIdS_PM = Math.max(Entreprise.FIRST_ID, getMaxIdOfTiers("'Entreprise'"));
			assertMaxId("S_PM", Entreprise.FIRST_ID, Entreprise.FIRST_ID, maxIdS_PM);
			updateSequence("S_PM", maxIdS_PM);
		}

		if (updateETBSequence) {
			final long maxIdS_ETB = Math.max(Etablissement.ETB_GEN_FIRST_ID, getMaxIdOfTiers("'Etablissement'"));
			assertMaxId("S_ETB", Etablissement.ETB_GEN_FIRST_ID, Etablissement.ETB_GEN_LAST_ID, maxIdS_ETB);
			updateSequence("S_ETB", maxIdS_ETB);
		}

		if (updateDPISequence) {
			final long maxIdDPI = Math.max(DebiteurPrestationImposable.FIRST_ID, getMaxIdOfTiers("'DebiteurPrestationImposable'"));
			assertMaxId("S_DPI", DebiteurPrestationImposable.FIRST_ID, DebiteurPrestationImposable.LAST_ID, maxIdDPI);
			updateSequence("S_DPI", maxIdDPI);
		}

// msi/jde : on n'a pas besoin de mettre-à-jour la séquence parce qu'elle est associée avec le FillHoleGenerator qui fonctionne différemment
//	         (en particulier elle est ré-initialisée dès qu'elle touche la limite supérieure)
//        final long maxIdS_CTB = getMaxIdOfTiers("'Habitant','NonHabitant','MenageCommun'");
//        Assert. isTrue(Contribuable.CTB_GEN_FIRST_ID <= maxIdS_CTB && maxIdS_CTB < Contribuable.CTB_GEN_LAST_ID);
//        updateSequence("S_CTB", maxIdS_CTB);
	}

	private static void assertMaxId(String sequence, long min, long max, long id) {
		if (id >= max) {
			throw new IllegalArgumentException(String.format("Séquence %s: l'ID maximum trouvé %d est en dehors de la plage de validité %d -> %d",
			                                                 sequence, id, min, max));
		}
	}

	private void updateSequence(String sequenceName, long maxId) {
		// Incrémente artificiellement la séquence jusqu'à ce que maxId soit atteint

		String sql = hibernateDialect.getSequenceNextValString(sequenceName);

		final JdbcTemplate template = new JdbcTemplate(dataSource);
		template.setIgnoreWarnings(false);

		long startId = template.queryForObject(sql, Long.class);

		if (hibernateDialect instanceof Oracle8iDialect) {
			if (startId < maxId) {
				long inc = maxId - startId + 1;

				// on augmente l'incrément de la valeur qui va bien
				String alter = "alter sequence " + sequenceName + " increment by " + inc;
				template.execute(alter);

				// on demande le prochain id pour mettre-à-jour la séquence
				String select = "select " + sequenceName + ".nextval from dual";
				long id = template.queryForObject(select, Long.class);
				if (id <= maxId) {
					throw new IllegalArgumentException();
				}

				// on reset l'incrément à 1
				alter = "alter sequence " + sequenceName + " increment by 1";
				template.execute(alter);
			}
		}
		else {
			long id;
			do {
				id = template.queryForObject(sql, Long.class);
			} while (id < maxId);
		}

		long newId = template.queryForObject(sql, Long.class);
		if (newId <= maxId) {
			throw new IllegalArgumentException();
		}
	}

	/**
	 * @return l'id max des entités hibernate actuellement présentes en base (à l'exception des entités de la table TIERS),
	 */
	private long getMaxIdOfAllHibernateEntities() {

		long max = -1;

		final JdbcTemplate template = new JdbcTemplate(dataSource);
		template.setIgnoreWarnings(false);

		for (final Table table : getHibernateTables()) {
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

			long id = template.queryForObject(sql, Long.class);
			max = (id > max ? id : max);
		}

		return max;
	}

	@NotNull
	private List<Table> getHibernateTables() {
		final Iterable<Namespace> namespaces = MetadataExtractorIntegrator.INSTANCE.getDatabase().getNamespaces();
		return StreamSupport.stream(namespaces.spliterator(), false)
				.map(Namespace::getTables)
				.flatMap(Collection::stream)
				.collect(Collectors.toList());
	}

	private long getMaxIdOfTiers(String listeEntite) {

		long max = -1;

		final JdbcTemplate template = new JdbcTemplate(dataSource);
		template.setIgnoreWarnings(false);

		final String sql = "select max(NUMERO) from TIERS where TIERS_TYPE in (" + listeEntite + ')';

		long id = template.queryForObject(sql, Long.class);
		max = (id > max ? id : max);

		return max;
	}

	@Override
	public void truncateDatabase() throws Exception {
		LOGGER.debug("Truncating database");
		try {
			sendTruncateDatabaseEvent();
			SqlFileExecutor.execute(transactionManager, dataSource, CORE_TRUNCATE_SQL);
		}
		catch (RuntimeException e) {
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

	@Override
	public String[] getTableNamesFromDatabase() {

		if (hibernateDialect instanceof Oracle8iDialect) {
			//noinspection SqlResolve
			final String sql = "SELECT table_name FROM user_tables";

			final JdbcTemplate template = new JdbcTemplate(dataSource);
			template.setIgnoreWarnings(false);

			final TransactionTemplate tmpl = new TransactionTemplate(transactionManager);
			final List<Map<String, Object>> rs = tmpl.execute(status -> template.queryForList(sql));

			List<String> list = new ArrayList<>();
			for (Map<String, Object> map : rs) {
				final String name = (String) map.values().iterator().next();
				list.add(name);
			}
			return list.toArray(new String[0]);
		}

		return null;
	}

	@Override
	public String[] getTableNamesFromHibernate(boolean reverse) {
		ArrayList<String> t = new ArrayList<>();
		for (Table table : getHibernateTables()) {
			if (table.isPhysicalTable()) {
				addTableName(t, table);
			}
		}
		if (reverse) {
			Collections.reverse(t);
		}
		return t.toArray(new String[0]);
	}

	@SuppressWarnings("unchecked")
	private void addTableName(ArrayList<String> tables, Table table) {
		if (tables.contains(table.getName())) {
			return;
		}
		for (Table t : getHibernateTables()) {
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

	@Override
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
					continue;
				}
				else if ("DOC_INDEX".equals(table)) {
					/*
					 * on ne dump pas la table des indexes de documents car les documents eux-mêmes restent sur le disque du serveur
					 */
					continue;
				}
				dataSet.addTable(table);
			}

			// XML file into which data needs to be extracted
			LOGGER.info("Début de l'export de la base de données.");
			XmlDataSet.write(dataSet, outputStream);
			LOGGER.info("La base de données à été exportée.");
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}
	}

	@Override
	public int dumpTiersListToDbunitFile(List<Long> tiersIds, final DumpParts parts, OutputStream outputStream, StatusManager status) throws Exception {

		status.setMessage("Analyse des données...");
		final Set<Long> allTiersIds = new HashSet<>();
		final Set<Long> allRETIds = new HashSet<>();
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

				otherTables = new ArrayList<>();

				final ITableIterator iter = q.iterator();
				while (iter.next()) {
					otherTables.add(iter.getTable());
				}
			}
			else {
				otherTables = Collections.emptyList();
			}

			if (status.isInterrupted()) {
				return -1;
			}

			// Les données des tiers et autres données appartenant aux tiers
			final List<ITable> tiersTables = queryDataSet(allTiersIds, connection, "Récupération des tiers", status, (ids, conn) -> queryTiersData(ids, parts, conn));

			if (status.isInterrupted()) {
				return -1;
			}

			final List<ITable> retTables;
			if (parts.rapportsEntreTiers && !allRETIds.isEmpty()) {
				// Les rapports-entre-tiers
				retTables = queryDataSet(allRETIds, connection, "Récupération des rapports-entre-tiers", status, DatabaseServiceImpl::queryRapportEntreTiersData);
			}
			else {
				retTables = Collections.emptyList();
			}

			final List<ITable> allTables = new ArrayList<>(tiersTables.size() + retTables.size());
			allTables.addAll(otherTables);
			allTables.addAll(tiersTables);
			allTables.addAll(retTables);

			final IDataSet dataSet = new CompositeDataSet(allTables.toArray(new ITable[0]));

			if (status.isInterrupted()) {
				return -1;
			}

			// XML file into which data needs to be extracted
			status.setMessage("Export des données en fichiers XML...");
			XmlDataSet.write(new ManagedDataSet(dataSet, status), outputStream);
			status.setMessage("Terminé.");

			return dataSet.getTable("TIERS").getRowCount();
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}
	}

	private interface QueryDataSetCallback {
		QueryDataSet execute(Collection<Long> ids, DatabaseConnection connection) throws SQLException;
	}

	private List<ITable> queryDataSet(Collection<Long> ids, DatabaseConnection connection, String message, StatusManager status, QueryDataSetCallback callback) throws SQLException, DataSetException {

		final List<ITable> tables = new ArrayList<>();

		final List<Long> idsList = new ArrayList<>(ids);
		Collections.sort(idsList);

		if (idsList.size() <= DEFAULT_BATCH_SIZE) {
			status.setMessage(message + "...");
			final QueryDataSet q = callback.execute(idsList, connection);

			final ITableIterator iter = q.iterator();
			while (iter.next() && !status.isInterrupted()) {
				tables.add(iter.getTable());
			}
		}
		else {
			// découpe par batchs
			final List<List<Long>> batches = split(idsList, DEFAULT_BATCH_SIZE);
			final int count = batches.size();

			for (int i = 0; i < count && !status.isInterrupted(); i++) {
				status.setMessage(message + " (lot " + i + '/' + count + ")...");
				final List<Long> batch = batches.get(i);
				final QueryDataSet q = callback.execute(batch, connection);

				final ITableIterator iter = q.iterator();
				while (iter.next() && !status.isInterrupted()) {
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
	 */
	private void determineAllRelatedIds(List<Long> inputTiersIds, Set<Long> relatedTiersIds, final Set<Long> relatedRETIds, DumpParts parts) {

		if (parts.rapportsEntreTiers) {

			final String sql =
					"select ID, TIERS_OBJET_ID, TIERS_SUJET_ID, TIERS_TUTEUR_ID from RAPPORT_ENTRE_TIERS where RAPPORT_ENTRE_TIERS_TYPE != 'RapportPrestationImposable' and TIERS_OBJET_ID in (:ids)"
							+
							" union select ID, TIERS_OBJET_ID, TIERS_SUJET_ID, TIERS_TUTEUR_ID from RAPPORT_ENTRE_TIERS where RAPPORT_ENTRE_TIERS_TYPE != 'RapportPrestationImposable' and TIERS_SUJET_ID in (:ids)";

			NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);

			Set<Long> newIds = new HashSet<>(inputTiersIds);

			// On recherche tous les ids en relation avec ceux spécifié. Pour cela il peut être nécessaire de faire 'n' passes en fonction de la profondeur des relations.
			for (; ;) {

				if (newIds.size() <= DEFAULT_BATCH_SIZE) {

					final List<Long> foundIds = new ArrayList<>();
					template.query(sql, Collections.singletonMap("ids", newIds), rs -> {
						relatedRETIds.add(rs.getLong(1)); // ID
						foundIds.add(rs.getLong(2)); // TIERS_OBJET_ID
						foundIds.add(rs.getLong(3)); // TIERS_SUJET_ID
						final long tuteurId = rs.getLong(4); // TIERS_TUTEUR_ID
						if (tuteurId > 0) {
							foundIds.add(tuteurId);
						}
					});

					if (relatedTiersIds.containsAll(foundIds)) {
						// tous les ids ont été trouvés -> c'est terminé
						break;
					}

					// extrait la liste des ids nouvellement trouvés
					newIds = new HashSet<>();
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
					final List<List<Long>> batches = split(new ArrayList<>(newIds), DEFAULT_BATCH_SIZE);
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
			final String sql = "select distinct(RETOUR_COLL_ADMIN_ID) from DOCUMENT_FISCAL where RETOUR_COLL_ADMIN_ID is not null and DOCUMENT_TYPE in ('DI', 'DIPM', 'QSNC', 'LR')";

			NamedParameterJdbcTemplate template = new NamedParameterJdbcTemplate(dataSource);
			@SuppressWarnings({"unchecked"})
			List<Long> ids = template.query(sql, new HashMap(), (RowMapper) (rs, rowNum) -> rs.getLong(1));

			relatedTiersIds.addAll(ids);
		}
	}

	private static <T> List<List<T>> split(List<T> list, int size) {

		List<List<T>> r = new ArrayList<>();

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

			final String query1 = String.format("select * from DOCUMENT_FISCAL where TIERS_ID in (%s) and DOCUMENT_TYPE in ('DI', 'DIPM', 'QSNC', 'LR')", idsList);
			dataSet.addTable("DOCUMENT_FISCAL", query1);

			final String query2 = String.format("select * from ETAT_DOCUMENT_FISCAL where DOCUMENT_FISCAL_ID in (select ID from DOCUMENT_FISCAL where TIERS_ID in (%s) and DOCUMENT_TYPE in ('DI', 'DIPM', 'QSNC', 'LR'))", idsList);
			dataSet.addTable("ETAT_DOCUMENT_FISCAL", query2);

			final String query3 = String.format("select * from DELAI_DOCUMENT_FISCAL where DOCUMENT_FISCAL_ID in (select ID from DOCUMENT_FISCAL where TIERS_ID in (%s) and DOCUMENT_TYPE in ('DI', 'DIPM', 'QSNC', 'LR'))", idsList);
			dataSet.addTable("DELAI_DOCUMENT_FISCAL", query3);
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
			b.append(id.toString()).append(',');
		}
		final String s = b.toString();
		return s.substring(0, s.length() - 1); // supprime la dernière virgule
	}

	@Override
	public void loadFromDbunitFile(InputStream inputStream, StatusManager status, boolean truncateBefore) throws Exception {

		Connection con = DataSourceUtils.getConnection(dataSource);
		try {
			DatabaseConnection connection = createNewDatabaseConnection(con);

			XmlDataSet dataSet = new XmlDataSet(inputStream);

			LOGGER.info("Début de l'import de la base de données.");

			final DatabaseOperation operation;
			if (truncateBefore) {
				sendTruncateDatabaseEvent();
				operation = new CompositeOperation(DatabaseOperation.DELETE_ALL, new ManagedInsertOperation(status));
			}
			else {
				operation = new ManagedInsertOperation(status);
			}

			operation.execute(connection, dataSet);
			sendLoadDatabaseEvent();

			LOGGER.info("La base de données à été importée.");
		}
		catch (Exception e) {
			LOGGER.error(e.getMessage(), e);
			throw e;
		}
		finally {
			DataSourceUtils.releaseConnection(con, dataSource);
		}
	}

	public void setHibernateDialect(Dialect hibernateDialect) {
		this.hibernateDialect = hibernateDialect;
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setFiscalDataEventNotifier(FiscalDataEventNotifier fiscalDataEventNotifier) {
		this.fiscalDataEventNotifier = fiscalDataEventNotifier;
	}
}
