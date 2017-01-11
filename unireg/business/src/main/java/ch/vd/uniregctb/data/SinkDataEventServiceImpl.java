package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Partie "sink" du service de notification des modifications de données
 */
public class SinkDataEventServiceImpl implements SinkDataEventService {

	private final List<SinkDataEventListener> listeners = new ArrayList<>();

	@Override
	public void register(SinkDataEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void onTiersChange(long id) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onTiersChange(id));
	}

	@Override
	public void onDroitAccessChange(long ppId) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onDroitAccessChange(ppId));
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onRelationshipChange(type, sujetId, objetId));
	}

	@Override
	public void onLoadDatabase() {
		DataEventServiceHelper.dispatch(listeners, SinkDataEventListener::onLoadDatabase);
	}

	@Override
	public void onTruncateDatabase() {
		DataEventServiceHelper.dispatch(listeners, SinkDataEventListener::onTruncateDatabase);
	}
}
