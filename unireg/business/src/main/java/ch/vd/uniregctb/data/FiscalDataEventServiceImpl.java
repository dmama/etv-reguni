package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Partie "fisale" du service de notification des modifications de données
 */
public class FiscalDataEventServiceImpl implements FiscalDataEventService, FiscalDataEventListener {

	private final List<FiscalDataEventListener> listeners = new ArrayList<>();

	@Override
	public void register(FiscalDataEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void onTiersChange(long id) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onTiersChange(id));
	}

	@Override
	public void onDroitAccessChange(long id) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onDroitAccessChange(id));
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onImmeubleChange(immeubleId));
	}

	@Override
	public void onBatimentChange(long batimentId) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onBatimentChange(batimentId));
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onRelationshipChange(type, sujetId, objetId));
	}

	@Override
	public void onLoadDatabase() {
		DataEventServiceHelper.dispatch(listeners, FiscalDataEventListener::onLoadDatabase);
	}

	@Override
	public void onTruncateDatabase() {
		DataEventServiceHelper.dispatch(listeners, FiscalDataEventListener::onTruncateDatabase);
	}
}
