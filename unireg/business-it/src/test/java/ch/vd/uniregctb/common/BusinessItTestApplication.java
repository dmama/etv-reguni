package ch.vd.uniregctb.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.util.zip.ZipInputStream;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.database.IDatabaseConnection;
import org.dbunit.dataset.IDataSet;
import org.dbunit.dataset.stream.StreamingDataSet;
import org.dbunit.dataset.xml.XmlProducer;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.ResourceUtils;
import org.xml.sax.InputSource;

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.database.DatabaseService;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer.Mode;


/**
 * Application stand-alone permettant de tester l'exécution tout ou partie d'Unireg dans l'environnement Business IT.
 */
public abstract class BusinessItTestApplication {

	private static final Logger LOGGER = Logger.getLogger(BusinessItTestApplication.class);

	protected DataSource dataSource;

	protected PlatformTransactionManager transactionManager;

	protected ClassPathXmlApplicationContext context;

	private DatabaseService databaseService;

	private GlobalTiersIndexer indexer;

	public BusinessItTestApplication() {

		// Le run pour de vrai
		boolean fileNotFound = true;
		{
			File file = new File("log4j.xml");
			if (file.exists()) {
				DOMConfigurator.configure("log4j.xml");
				fileNotFound = false;
			}
		}
		// Dans Eclipse
		if (fileNotFound) {
			File file = new File("src/test/resources/ut/log4j.xml");
			if (file.exists()) {
				DOMConfigurator.configure("src/test/resources/ut/log4j.xml");
			}
			else {
				Assert.fail("Pas de fichier Log4j");
			}
		}
	}

	protected void run() throws Exception {

		String[] files = {
				TestingConstants.UNIREG_CORE_DAO, 
				TestingConstants.UNIREG_CORE_SF, 
				TestingConstants.UNIREG_CORE_UT_DATASOURCE,
				TestingConstants.UNIREG_CORE_UT_PROPERTIES,
				TestingConstants.UNIREG_BUSINESS_UT_CACHE,
				TestingConstants.UNIREG_BUSINESS_INTERFACES,
				TestingConstants.UNIREG_BUSINESS_SERVICES,
				// TestingConstants.UNIREG_BUSINESS_MDP,
				// TestingConstants.UNIREG_BUSINESS_JOBS,
				TestingConstants.UNIREG_BUSINESS_EVT_CIVIL, TestingConstants.UNIREG_BUSINESSIT_INTERFACES,
				TestingConstants.UNIREG_BUSINESS_UT_SERVICES,
				TestingConstants.UNIREG_BUSINESS_UT_APIREG,
				TestingConstants.UNIREG_BUSINESS_UT_CLIENT_WEBSERVICE,
				// TestingConstants.UNIREG_BUSINESS_UT_INTERFACES,
				TestingConstants.UNIREG_BUSINESS_UT_EDITIQUE
		// TestingConstants.UNIREG_BUSINESS_UT_JOBS
		};

		context = new ClassPathXmlApplicationContext(files);
		context.registerShutdownHook();

		dataSource = (DataSource) context.getBean("dataSource");
		transactionManager = (PlatformTransactionManager) context.getBean("transactionManager");
		databaseService = (DatabaseService) context.getBean("databaseService");
		indexer = (GlobalTiersIndexer) context.getBean("globalTiersIndexer");
		Assert.notNull(databaseService);
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

				String name = "classpath:" + packageName + "/" + filename;
				file = ResourceUtils.getFile(name);
			}
			catch (Exception ignored) {
				// La variable file est nulle, ca nous suffit
			}
		}
		loadDataSet(file);

		databaseService.ensureSequencesUpToDate(true, true, true);
	}

	/**
	 * Charge un fichier Dbunit dans la database préalablement vidée, le fichier doit être du format {@link #getProducerType()}.
	 *
	 * @param file
	 *            le fichier à utiliser
	 */
	private void loadDataSet(final File file) throws Exception {

		final String filepath = file.toURL().toString();
		LOGGER.info("Loading database from file = " + filepath);

		if (filepath.endsWith(".zip")) {
			// fichier zip -> on assume qu'il n'y a qu'un seul fichier à l'intérieur et que c'est le fichier DBUnit
			InputStream is = null;
			ZipInputStream zipstream = null;
			try {
				is = new FileInputStream(file);
				zipstream = new ZipInputStream(is);
				zipstream.getNextEntry();
				loadDataSet(zipstream);
			}
			catch (Exception e) {
				LOGGER.error(e, e);
				throw e;
			}
			finally {
				if (zipstream != null) {
					zipstream.close();
				}
				if (is != null) {
					is.close();
				}
			}
		}
		else {
			// autre type de fichier -> on assume que c'est le fichier DBUnit
			InputStream is = null;
			try {
				is = new FileInputStream(file);
				loadDataSet(is);
			}
			catch (Exception e) {
				LOGGER.error(e, e);
				throw e;
			}
			finally {
				if (is != null) {
					is.close();
				}
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
		indexer.indexAllDatabaseAsync(null, 4, Mode.INCREMENTAL, false);
	}
}
