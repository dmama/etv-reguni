package ch.vd.unireg.testing;

import javax.sql.DataSource;
import java.io.InputStream;
import java.sql.Connection;

import org.dbunit.database.DatabaseConfig;
import org.dbunit.database.DatabaseConnection;
import org.dbunit.dataset.xml.XmlDataSet;
import org.dbunit.ext.oracle.OracleDataTypeFactory;
import org.dbunit.operation.DatabaseOperation;
import org.hibernate.SessionFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.utils.Assert;
import ch.vd.unireg.dbutils.SqlFileExecutor;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.tiers.TiersDAO;

public abstract class InContainerTest {

	protected static final String CORE_TRUNCATE_SQL = "/sql/core_truncate_tables.sql";

	private TiersDAO tiersDAO;
	private DataSource dataSource;
	private GlobalTiersIndexer globalTiersIndexer;
	private PlatformTransactionManager transactionManager;
	private SessionFactory sessionFactory;

	public void onSetUp() throws Exception {
		SqlFileExecutor.execute(transactionManager, dataSource, CORE_TRUNCATE_SQL);
	}

	public void onTearDown() throws Exception {
	}

	protected void loadDatabase(String filename) throws Exception {

		InputStream inputStream = getClass().getResourceAsStream(filename);
		Assert.notNull(inputStream, "Le fichier DBUnit '" + filename + "' est invalide");
		XmlDataSet dataSet = new XmlDataSet(inputStream);

		Assert.notNull(dataSet);
		try (Connection connection = dataSource.getConnection()) {
			DatabaseConnection con = new DatabaseConnection(connection);
			DatabaseConfig config = con.getConfig();
			config.setProperty(DatabaseConfig.PROPERTY_DATATYPE_FACTORY, new OracleDataTypeFactory());

			DatabaseOperation.CLEAN_INSERT.execute(con, dataSet);
		}
		globalTiersIndexer.indexAllDatabase();
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
	}

	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		this.globalTiersIndexer = globalTiersIndexer;
	}

	protected TiersDAO getTiersDAO() {
		return tiersDAO;
	}

	public void setTiersDAO(TiersDAO tiersDAO) {
		this.tiersDAO = tiersDAO;
	}

	protected PlatformTransactionManager getTransactionManager() {
		return transactionManager;
	}

	public void setTransactionManager(PlatformTransactionManager platformTransactionManager) {
		this.transactionManager = platformTransactionManager;
	}

	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	protected SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public <T> T executeInTransaction(TransactionCallback<T> action) {
		TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionTemplate.PROPAGATION_REQUIRES_NEW);
		return template.execute(action);
	}
}
