package ch.vd.uniregctb.common;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.sql.DataSource;

import org.dbunit.DatabaseUnitException;
import org.dbunit.database.CachedResultSetTableFactory;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.ForwardOnlyResultSetTableFactory;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.database.IResultSetTableFactory;
import org.dbunit.database.QueryDataSet;
import org.dbunit.dataset.CachedDataSet;
import org.dbunit.dataset.CompositeDataSet;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.csv.CsvProducer;
import org.dbunit.dataset.stream.IDataSetProducer;
import org.dbunit.dataset.stream.StreamingDataSet;
import org.dbunit.dataset.xml.FlatDtdProducer;
import org.dbunit.dataset.xml.FlatXmlProducer;
import org.dbunit.dataset.xml.XmlDataSetWriter;
import org.dbunit.dataset.xml.XmlProducer;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.mapping.ForeignKey;
import org.hibernate.mapping.Table;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.hibernate3.LocalSessionFactoryBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.jdbc.SimpleJdbcTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.util.Log4jConfigurer;
import org.springframework.util.ResourceUtils;
import org.xml.sax.InputSource;

/**
 * Test case abstrait permettant de tester les DAO Spring.
 *
 * @author <a href="mailto:akram.ben-aissi@vd.ch">Akram BEN AISSI</a>
 */
@ContextConfiguration(locations = {
		TestingConstants.UNIREG_CORE_DAO,
		TestingConstants.UNIREG_CORE_SF,
		TestingConstants.UNIREG_CORE_UT_DATASOURCE,
		TestingConstants.UNIREG_CORE_UT_PROPERTIES
})
public abstract class AbstractCoreDAOTest extends AbstractSpringTest {

	static {
		try {
			Log4jConfigurer.initLogging(getLog4jConfigFilename().toString());
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Fichier de configuration par défaut de log4j.
	 */
	private static final String DEFAULT_LOG4J_CONFIGURATION_FILENAME = "classpath:ut/log4j.xml";

	protected DataSource dataSource;

	protected SimpleJdbcTemplate simpleJdbcTemplate;

	protected LocalSessionFactoryBean localSessionFactoryBean;

	private ProducerType producerType = ProducerType.Xml;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		localSessionFactoryBean = getBean(LocalSessionFactoryBean.class, "&sessionFactory");
		setDataSource(getBean(DataSource.class, "dataSource"));

		try {
			truncateDatabase();
			doLoadDatabase();
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	// Can be overriden
	protected void doLoadDatabase() throws Exception {
	}


	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.simpleJdbcTemplate = new SimpleJdbcTemplate(dataSource);
	}

	/**
	 * @return the producerType
	 */
	public ProducerType getProducerType() {
		return producerType;
	}

	/**
	 * @param producerType
	 *            the producerType to set
	 */
	public void setProducerType(ProducerType producerType) {
		this.producerType = producerType;
	}

	/**
	 * virtual method to truncate the database
	 */
	protected void truncateDatabase() throws Exception {
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				deleteFromTables(getTableNames(false));
				return null;
			}
		});
	}

	@SuppressWarnings("unchecked")
	public String[] getTableNames(boolean reverse) {
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
	 * Charge le fichier DbUnit après avoir vidé la base de données
	 */
	protected void loadDatabase(final String filename) throws Exception {

		File file = null;

		// Essaie d'abord tel-quel
		try {
			file = ResourceUtils.getFile(filename);
		}
		catch (Exception ignored) {
			// La variable file est nulle, ca nous suffit
		}

		// Ensuite avec classpath: devant
		if (file == null || !file.exists()) {
			try {
				String name = "classpath:"+filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}

		// Ensuite avec classpath: et le chemin du package devant
		if (file == null || !file.exists()) {
			try {
				String packageName = getClass().getPackage().getName();
				packageName = packageName.replace('.', '/');

				String name = "classpath:"+packageName+"/"+filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}
		loadDataSet(file);
	}

	/**
	 * Charge un fichier Dbunit dans la database préalablement vidée, le fichier doit être du format {@link #getProducerType()}.
	 *
	 * @param file
	 *            le fichier à utiliser
	 */
	private void loadDataSet(final File file) throws Exception {

		doInNewTransaction(new TxCallback() {

			@Override
			public Object execute(TransactionStatus status) throws Exception {

				//Assert.assertTrue(file != null && file.exists());

				// initialize your database connection here
				Connection sql = DataSourceUtils.getConnection(dataSource);
				// initialize your dataset here
				try {
					IDatabaseConnection connection = createNewConnection(sql);
					IDataSet dataSet = getSrcDataSet(file, getProducerType(), false);
					DatabaseOperation.INSERT.execute(connection, dataSet);
				}
				catch (Exception e) {
					throw new RuntimeException(e);
				}
				finally {
					DataSourceUtils.releaseConnection(sql, dataSource);
				}
				return null;
			}
		});

	}

	/**
	 * Permet d'extraire les données de la db pour un fichier DBUnit de type {@link #getProducerType()}
	 * @param filename le nom du fichier
	 * @throws Exception exception
	 */
	public void dumpDatabase(String filename) throws Exception {
		dumpDatabase(new FileWriter(filename));
	}

	/**
	 * Permet d'extraire les données de la db pour un fichier DBUnit de type {@link #getProducerType()}
	 * @param writer writer
	 * @throws Exception exception
	 */
	public void dumpDatabase(Writer writer) throws Exception {
		// full database export
		Connection dsCon = DataSourceUtils.getConnection( dataSource);
		try {
			DatabaseConnection connection = createNewConnection(dsCon);
			QueryDataSet dataSet = new QueryDataSet(connection);
			for (String tablename : getTableNames(false)) {
				dataSet.addTable(tablename);
			}
			XmlDataSetWriter dataSetWriter = new XmlDataSetWriter(writer, "UTF-8");
			dataSetWriter.write(dataSet);

		}
		finally {
			DataSourceUtils.releaseConnection(dsCon, dataSource);
		}
	}

	protected static URL getLog4jConfigFilename() throws Exception {
		return ResourceUtils.getURL(DEFAULT_LOG4J_CONFIGURATION_FILENAME);
	}

	protected static DatabaseConnection createNewConnection(Connection connection) {
		final DatabaseConnection dbConnection = new DatabaseConnection(connection);
		final DatabaseConfig config = dbConnection.getConfig();
		config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new OracleDataTypeFactory());
		return dbConnection;
	}

	/**
	 * Convenience method for deleting all rows from the specified tables. Use with caution outside of a transaction!
	 *
	 * @param names
	 *            the names of the tables from which to delete
	 * @return the total number of rows deleted from all specified tables
	 */
	protected int deleteFromTables(String... names) {
		return SimpleJdbcTestUtils.deleteFromTables(this.simpleJdbcTemplate, names);
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
		while ( ts.hasNext()) {
			Table t = ts.next();
			if ( t.equals(table)){
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



	/**
	 *
	 * @param connection
	 * @param tables
	 * @param forwardonly
	 * @return
	 * @throws DatabaseUnitException
	 */
	protected IDataSet getDatabaseDataSet(IDatabaseConnection connection, String[] tables, boolean forwardonly)
			throws DatabaseUnitException {
		if (logger.isDebugEnabled()) {
			logger.debug("getDatabaseDataSet(connection=" + connection + ", tables=" + tables + ", forwardonly=" + forwardonly
					+ ") - start");
		}

		try {
			// Setup the ResultSet table factory
			IResultSetTableFactory factory = null;
			if (forwardonly) {
				factory = new ForwardOnlyResultSetTableFactory();
			}
			else {
				factory = new CachedResultSetTableFactory();
			}
			DatabaseConfig config = connection.getConfig();
			config.setProperty(DatabaseConfig.PROPERTY_RESULTSET_TABLE_FACTORY, factory);

			// Retrieve the complete database if no tables or queries specified.
			if (tables == null || tables.length == 0) {
				return connection.createDataSet();
			}

			List<QueryDataSet> queryDataSets = new ArrayList<QueryDataSet>();

			QueryDataSet queryDataSet = new QueryDataSet(connection);

			for (String item : tables) {
				queryDataSet.addTable(item);
			}

			if (queryDataSet.getTableNames().length > 0)
				queryDataSets.add(queryDataSet);

			IDataSet[] dataSetsArray = new IDataSet[queryDataSets.size()];
			return new CompositeDataSet(queryDataSets.toArray(dataSetsArray));
		}
		catch (SQLException e) {
			logger.error("getDatabaseDataSet()", e);

			throw new DatabaseUnitException(e);
		}
	}

	/**
	 *
	 * @param src
	 * @param format
	 * @param forwardonly
	 * @return
	 * @throws DatabaseUnitException
	 */
	protected IDataSet getSrcDataSet(File src, ProducerType format, boolean forwardonly) throws DatabaseUnitException {
		logger.debug("getSrcDataSet(src=" + src + ", format=" + format + ", forwardonly=" + forwardonly + ") - start");

		try {
			IDataSetProducer producer = null;
			if (format == ProducerType.Xml) {
				producer = new XmlProducer(new InputSource(src.toURL().toString()));
			}
			else if (format == ProducerType.Cvs) {
				producer = new CsvProducer(src);
			}
			else if (format == ProducerType.Flat) {
				producer = new FlatXmlProducer(new InputSource(src.toURL().toString()));
			}
			else if (format == ProducerType.Dtd) {
				producer = new FlatDtdProducer(new InputSource(src.toURL().toString()));
			}
			else {
				throw new IllegalArgumentException("Type must be either 'flat'(default), 'xml', 'csv' or 'dtd' but was: " + format);
			}

			if (forwardonly) {
				return new StreamingDataSet(producer);
			}
			return new CachedDataSet(producer);
		}
		catch (IOException e) {
			logger.error("getSrcDataSet()", e);

			throw new DatabaseUnitException(e);
		}
	}

	// Facilite l'import des methodes assertXXX
	/*
	import static junit.framework.Assert.assertEquals;
	import static junit.framework.Assert.fail;
	import static junit.framework.Assert.assertTrue;
	import static junit.framework.Assert.assertFalse;
	import static junit.framework.Assert.assertNotNull;
	import static junit.framework.Assert.assertNull;
	import static junit.framework.Assert.assertSame;
	import static junit.framework.Assert.failNotEquals;
	*/

	/*
		doInNewTransaction(new TxCallback() {
			@Override
			public Object execute(TransactionStatus status) throws Exception {
				...
				return null;
			}
		});
	*/

}
