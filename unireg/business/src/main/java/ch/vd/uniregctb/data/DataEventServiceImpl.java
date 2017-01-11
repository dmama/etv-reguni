package ch.vd.uniregctb.data;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class DataEventServiceImpl implements DataEventService {

	private final SourceDataEventService source = new SourceDataEventServiceImpl();
	private final SinkDataEventService sink = new SinkDataEventServiceImpl();

	@Override
	public void register(SourceDataEventListener listener) {
		source.register(listener);
	}

	@Override
	public void register(SinkDataEventListener listener) {
		sink.register(listener);
	}

	@Override
	public void onTruncateDatabase() {
		sink.onTruncateDatabase();
	}

	@Override
	public void onLoadDatabase() {
		sink.onLoadDatabase();
	}

	@Override
	public void onTiersChange(long id) {
		sink.onTiersChange(id);
	}

	@Override
	public void onIndividuChange(long id) {
		source.onIndividuChange(id);
	}

	@Override
	public void onOrganisationChange(long id) {
		source.onOrganisationChange(id);
	}

	@Override
	public void onDroitAccessChange(long ppId) {
		sink.onDroitAccessChange(ppId);
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		sink.onRelationshipChange(type, sujetId, objetId);
	}
}
