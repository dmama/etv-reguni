package ch.vd.uniregctb.data;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class DataEventServiceImpl implements DataEventService {

	private final CivilDataEventService civil = new CivilDataEventServiceImpl();
	private final FiscalDataEventService fiscal = new FiscalDataEventServiceImpl();

	@Override
	public void register(CivilDataEventListener listener) {
		civil.register(listener);
	}

	@Override
	public void register(FiscalDataEventListener listener) {
		fiscal.register(listener);
	}

	@Override
	public void onTruncateDatabase() {
		fiscal.onTruncateDatabase();
	}

	@Override
	public void onLoadDatabase() {
		fiscal.onLoadDatabase();
	}

	@Override
	public void onTiersChange(long id) {
		fiscal.onTiersChange(id);
	}

	@Override
	public void onIndividuChange(long id) {
		civil.onIndividuChange(id);
	}

	@Override
	public void onOrganisationChange(long id) {
		civil.onOrganisationChange(id);
	}

	@Override
	public void onDroitAccessChange(long ppId) {
		fiscal.onDroitAccessChange(ppId);
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		fiscal.onRelationshipChange(type, sujetId, objetId);
	}
}
