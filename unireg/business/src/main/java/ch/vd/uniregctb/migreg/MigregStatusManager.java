package ch.vd.uniregctb.migreg;

import ch.vd.uniregctb.common.StatusManager;

public class MigregStatusManager implements StatusManager {

	//private static final Logger LOGGER = Logger.getLogger(MigregStatusManager.class);

	/**
	 * Le nombre d'objects committé dans la base Oracle
	 */
	private int globalNbObjectsMigrated = 0;

	public void reset() {
		globalNbObjectsMigrated = 0;
	}

	public synchronized Integer getGlobalNbObjectsMigrated() {
		return globalNbObjectsMigrated;
	}
	public int addGlobalObjectsMigrated(int nb)  {
		globalNbObjectsMigrated += nb;
		return globalNbObjectsMigrated;
	}

	public boolean interrupted() {
		return false;
	}

	public void setMessage(String msg) {
	}

	public void setMessage(String msg, int percentProgression) {
	}
}
