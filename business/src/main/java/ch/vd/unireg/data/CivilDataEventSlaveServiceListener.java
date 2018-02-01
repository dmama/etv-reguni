package ch.vd.unireg.data;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Ce listener s'enregistre dans un service maître, et délégue tous les appels à un listener esclave.
 */
public class CivilDataEventSlaveServiceListener implements CivilDataEventListener, InitializingBean, DisposableBean {

	private CivilDataEventService master;
	private CivilDataEventListener slave;

	public void setMaster(CivilDataEventService master) {
		this.master = master;
	}

	public void setSlave(CivilDataEventListener slave) {
		this.slave = slave;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		master.register(this);
	}

	@Override
	public void destroy() throws Exception {
		master.unregister(this);
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
