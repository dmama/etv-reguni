package ch.vd.unireg.data;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Cette classe s'enregistre comme listener dans un service maître et délégue tous les appels à un listener esclave.
 */
public class CivilDataEventBridge implements CivilDataEventListener, InitializingBean, DisposableBean {

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
	public void onEntrepriseChange(long id) {
		slave.onEntrepriseChange(id);
	}

	@Override
	public void onIndividuChange(long id) {
		slave.onIndividuChange(id);
	}
}
