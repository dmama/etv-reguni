package ch.vd.uniregctb.interfaces.migreg;


import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.migreg.HostMigrationManager;
import ch.vd.uniregctb.migreg.LimitsConfigurator;
import ch.vd.uniregctb.migreg.MigRegLimits;
import ch.vd.uniregctb.migreg.MigRegLimitsList;
import ch.vd.uniregctb.migreg.MigregStatusManager;


public  class Migrator{


	protected GlobalTiersIndexer globalTiersIndexer;

	protected HostMigrationManager hostMigrationManager;

	protected PlatformTransactionManager transactionManager;

	protected String db2Schema;



	/**
	 * @throws Exception
	 */


	protected int executeMigration(MigRegLimitsList limitsList, int nbBeforeCommit) throws Exception {

		int savedValue = hostMigrationManager.getNbrInsertBeforeCommit();
		hostMigrationManager.setNbrInsertBeforeCommit(nbBeforeCommit);

		MigregStatusManager mgr = new MigregStatusManager();
		int nb = hostMigrationManager.execute(limitsList, mgr);
		hostMigrationManager.setNbrInsertBeforeCommit(savedValue);

		globalTiersIndexer.flush();

		return nb;
	}



	public void setGlobalTiersIndexer(GlobalTiersIndexer globalTiersIndexer) {
		this.globalTiersIndexer = globalTiersIndexer;
	}



	public void setHostMigrationManager(HostMigrationManager hostMigrationManager) {
		this.hostMigrationManager = hostMigrationManager;
	}



	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}



	public void setDb2Schema(String db2Schema) {
		this.db2Schema = db2Schema;
	}

	public void migreHabitant(int numHabitantMin,int numHabitantMax) throws Exception{
	MigRegLimitsList limitsList = new MigRegLimitsList("MigregUniregInterface");
	{
		MigRegLimits limit = new MigRegLimits();
		limit.setOrdinaire(numHabitantMin, numHabitantMax);
		limit.setWantCouple(false);
		limitsList.append(limit);
	}
	executeMigration(limitsList, 20);
}

	public void migreCouple() throws Exception{
		MigRegLimitsList limitsList = new MigRegLimitsList("MigregUniregInterface");
		{
			MigRegLimits limit = new MigRegLimits();
			limit.setOrdinaire(57109210, 57109210);
			limit.setWantCouple(false);
			limitsList.append(limit);
		}
		{
			MigRegLimits limit = new MigRegLimits();
			limit.setOrdinaire(75107103, 75107103);
			limit.setWantCouple(false);
			limitsList.append(limit);
		}
		{
			MigRegLimits limit = new MigRegLimits();
			limit.setOrdinaire(10099496, 10099496);
			limit.setWantCouple(true);
			limitsList.append(limit);
		}
		executeMigration(limitsList, 20);
	}

	public void migreDonneesAnnie() throws Exception{
		executeMigration(LimitsConfigurator.cfg(LimitsConfigurator.ANNIE, null), 10);
	}
}
