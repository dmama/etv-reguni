package ch.vd.uniregctb.migreg;

import ch.vd.uniregctb.common.StatusManager;

public abstract class SubElementsFetcher {

	protected final HostMigratorHelper helper;
	protected final StatusManager mgr;

	public SubElementsFetcher(HostMigratorHelper helper, StatusManager mgr) {

		this.helper = helper;
		this.mgr = mgr;
	}

}
