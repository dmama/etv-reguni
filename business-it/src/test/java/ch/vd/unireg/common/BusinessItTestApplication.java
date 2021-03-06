package ch.vd.unireg.common;

import javax.sql.DataSource;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.zip.ZipInputStream;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.stream.StreamingDataSet;
import org.dbunit.dataset.xml.XmlProducer;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ResourceUtils;
import org.xml.sax.InputSource;

import ch.vd.unireg.database.DatabaseService;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer.Mode;

import static org.junit.Assert.assertNotNull;


/**
 * Application stand-alone permettant de tester l'exécution tout ou partie d'Unireg dans l'environnement Business IT.
 */
public abstract class BusinessItTestApplication {

	private static final Logger LOGGER = LoggerFactory.getLogger(BusinessItTestApplication.class);

	protected DataSource dataSource;

	protected PlatformTransactionManager transactionManager;

	protected ClassPathXmlApplicationContext context;

	private DatabaseService databaseService;

	private GlobalTiersIndexer indexer;

	protected void run() throws Exception {

		String[] files = {
				CoreTestingConstants.UNIREG_CORE_DAO,
				CoreTestingConstants.UNIREG_CORE_SF,
				CoreTestingConstants.UNIREG_CORE_UT_DATASOURCE,
				CoreTestingConstants.UNIREG_CORE_UT_PROPERTIES,
				BusinessTestingConstants.UNIREG_BUSINESS_UT_CACHE,
				BusinessTestingConstants.UNIREG_BUSINESS_UT_INTERFACES,
				BusinessTestingConstants.UNIREG_BUSINESS_ESSENTIALS,
				BusinessTestingConstants.UNIREG_BUSINESS_UT_EVT_ENTREPRISE,
				BusinessTestingConstants.UNIREG_BUSINESS_SERVICES,
				BusinessTestingConstants.UNIREG_BUSINESS_CXF,
				BusinessTestingConstants.UNIREG_BUSINESS_EVT_CIVIL,
				BusinessTestingConstants.UNIREG_BUSINESS_EVT_FISCAL,
				BusinessTestingConstants.UNIREG_BUSINESS_UT_SERVICES,
				BusinessTestingConstants.UNIREG_BUSINESS_UT_CLIENT_WEBSERVICE,
				BusinessTestingConstants.UNIREG_BUSINESS_UT_EDITIQUE,
				BusinessTestingConstants.UNIREG_BUSINESS_UT_JMS,
				BusinessItTestingConstants.UNIREG_BUSINESSIT_INTERFACES,
				BusinessItTestingConstants.UNIREG_BUSINESSIT_CONNECTORS,
				BusinessItTestingConstants.UNIREG_BUSINESSIT_EXT_INTERFACES,
				BusinessItTestingConstants.UNIREG_BUSINESSIT_EXT_INTERFACES_REFSEC,
		};

		context = new ClassPathXmlApplicationContext(files);
		context.registerShutdownHook();

		dataSource = (DataSource) context.getBean("dataSource");
		transactionManager = (PlatformTransactionManager) context.getBean("transactionManager");
		databaseService = (DatabaseService) context.getBean("databaseService");
		indexer = (GlobalTiersIndexer) context.getBean("globalTiersIndexer");
		assertNotNull(databaseService);
	}

	protected void clearDatabase() throws Exception {
		databaseService.truncateDatabase();
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
				String name = "classpath:" + filename;
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

				String name = "classpath:" + packageName + '/' + filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}
		loadDataSet(file);

		databaseService.ensureSequencesUpToDate(true, true, true, true, true);
	}

	/**
	 * Charge un fichier Dbunit dans la database préalablement vidée, le fichier doit être au format DBUnit.
	 *
	 * @param file le fichier à utiliser
	 */
	private void loadDataSet(final File file) throws Exception {

		final String filepath = file.toURI().toURL().toString();
		LOGGER.info("Loading database from file = " + filepath);

		if (filepath.endsWith(".zip")) {
			// fichier zip -> on assume qu'il n'y a qu'un seul fichier à l'intérieur et que c'est le fichier DBUnit
			try (InputStream is = new FileInputStream(file); ZipInputStream zipstream = new ZipInputStream(is)) {
				zipstream.getNextEntry();
				loadDataSet(zipstream);
			}
			catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				throw e;
			}
		}
		else {
			// autre type de fichier -> on assume que c'est le fichier DBUnit
			try (InputStream is = new FileInputStream(file)) {
				loadDataSet(is);
			}
			catch (Exception e) {
				LOGGER.error(e.getMessage(), e);
				throw e;
			}
		}
	}

	private void loadDataSet(InputStream is) {
		// initialize your database connection here
		Connection sql = DataSourceUtils.getConnection(dataSource);
		// initialize your dataset here
		try {
			IDatabaseConnection connection = new DatabaseConnection(sql);
			DatabaseConfig config = connection.getConfig();
			config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new OracleDataTypeFactory());

			XmlProducer producer = new XmlProducer(new InputSource(is));
			IDataSet dataSet = new StreamingDataSet(producer);
			DatabaseOperation.INSERT.execute(connection, dataSet);
		}
		catch (Exception e) {
			throw new RuntimeException(e);
		}
		finally {
			DataSourceUtils.releaseConnection(sql, dataSource);
		}
	}

	protected void reindexDatabase() {
		indexer.indexAllDatabase(Mode.MISSING_ONLY, 4, null);
	}
}
