package ch.vd.uniregctb.migreg;

import junit.framework.Assert;

import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.common.BusinessItTest;
import ch.vd.uniregctb.common.ClientConstants;
import ch.vd.uniregctb.tiers.TiersDAO;

@ContextConfiguration(locations = {
		ClientConstants.UNIREG_BUSINESS_MIGREG }
)
public abstract class MigregTest extends BusinessItTest {

	protected HostMigrationManager hostMigrationManager;
	protected MigrationErrorDAO migrationErrorDAO;
	protected TiersDAO tiersDAO;
	protected PlatformTransactionManager transactionManager;
	protected String db2Schema;
	protected int nbCollAdminCreated;

	@Override
	public void onSetUp() throws Exception {
		super.onSetUp();

		hostMigrationManager = getBean(HostMigrationManager.class, "hostMigrationManager");
		migrationErrorDAO = getBean(MigrationErrorDAO.class, "migrationErrorDAO");
		tiersDAO = getBean(TiersDAO.class, "tiersDAO");
		transactionManager = getBean(PlatformTransactionManager.class, "transactionManager");
		db2Schema = getBean(String.class, "db2Schema");

		truncateDatabase();

		Assert.assertEquals("CIIV1", db2Schema);
	}

	protected int executeMigration(MigRegLimitsList limitsList, int nbBeforeCommit) throws Exception {

		int savedValue = hostMigrationManager.getNbrInsertBeforeCommit();
		hostMigrationManager.setNbrInsertBeforeCommit(nbBeforeCommit);

		MigregStatusManager mgr = new MigregStatusManager();
		int nb = hostMigrationManager.execute(limitsList, mgr);
		nbCollAdminCreated = hostMigrationManager.getNbCollAdminCreated();
		hostMigrationManager.setNbrInsertBeforeCommit(savedValue);

		globalTiersIndexer.flush();

		return nb;
	}

}
