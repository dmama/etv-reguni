package ch.vd.uniregctb.data;

import org.springframework.beans.factory.InitializingBean;

/**
 * Ce listener s'enregistre dans un service maître, et délégue tous les appels à un service esclave.
 */
public class CivilDataEventSlaveServiceListener implements CivilDataEventListener, InitializingBean {

	private CivilDataEventService master;
	private CivilDataEventService slave;

	public void setMaster(CivilDataEventService master) {
		this.master = master;
	}

	public void setSlave(CivilDataEventService slave) {
		this.slave = slave;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		master.register(this);
	}

	@Override
	public void onOrganisationChange(long id) {
		slave.onOrganisationChange(id);
	}

	@Override
	public void onIndividuChange(long id) {
		slave.onIndividuChange(id);
	}
}
