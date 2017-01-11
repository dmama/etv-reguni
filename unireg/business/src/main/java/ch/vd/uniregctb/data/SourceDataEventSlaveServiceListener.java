package ch.vd.uniregctb.data;

import org.springframework.beans.factory.InitializingBean;

/**
 * Ce listener s'enregistre dans un service maître, et délégue tous les appels à un service esclave.
 */
public class SourceDataEventSlaveServiceListener implements SourceDataEventListener, InitializingBean {

	private SourceDataEventService master;
	private SourceDataEventService slave;

	public void setMaster(SourceDataEventService master) {
		this.master = master;
	}

	public void setSlave(SourceDataEventService slave) {
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
