package ch.vd.uniregctb.migreg;

import org.apache.log4j.Logger;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.migreg.HostMigratorHelper.IndexationMode;

public class HostMigratorThread  extends Thread {

	private static final Logger LOGGER = Logger.getLogger(HostMigratorThread.class);

	private final HostMigrationManager hmm;
	private final MigRegLimits limits;
	private final IndexationMode mode;
	private final MigregStatusManager status;

	public HostMigratorThread(HostMigrationManager hmm, MigRegLimits limits, IndexationMode mode, MigregStatusManager mgr) {
		this.hmm = hmm;
		this.limits = limits;
		this.mode= mode;
		this.status = mgr;
	}

	@Override
	public void run() {

		AuthenticationHelper.setPrincipal("[HostImpotSourceMigrator thread]");
		try {
			hmm.internalExecute(limits, mode, status);
			//Audit.info("MigReg : Fin du traitement "+limits);
		}
		catch (Exception e) {
			LOGGER.error(e, e);
			String message = "Problème lors de la migration. Exception=" + e.getMessage();
			Audit.error(message);
		}
		AuthenticationHelper.setAuthentication(null);
	}

}
