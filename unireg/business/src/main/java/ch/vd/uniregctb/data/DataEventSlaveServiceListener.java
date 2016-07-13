package ch.vd.uniregctb.data;

import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Ce listener s'enregistre dans un service maître, et délégue tous les appels à un service esclave.
 */
public class DataEventSlaveServiceListener implements DataEventListener, InitializingBean {

	private DataEventService master;
	private DataEventService slave;

	public void setMaster(DataEventService master) {
		this.master = master;
	}

	public void setSlave(DataEventService slave) {
		this.slave = slave;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		master.register(this);
	}

	@Override
	public void onTiersChange(long id) {
		slave.onTiersChange(id);
	}

	@Override
	public void onOrganisationChange(long id) {
		slave.onOrganisationChange(id);
	}

	@Override
	public void onIndividuChange(long id) {
		slave.onIndividuChange(id);
	}

	@Override
	public void onDroitAccessChange(long tiersId) {
		slave.onDroitAccessChange(tiersId);
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		slave.onRelationshipChange(type, sujetId, objetId);
	}

	@Override
	public void onTruncateDatabase() {
		slave.onTruncateDatabase();
	}

	@Override
	public void onLoadDatabase() {
		slave.onLoadDatabase();
	}
}
