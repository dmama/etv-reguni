package ch.vd.unireg.data;

import java.util.List;

import ch.vd.unireg.type.TypeRapportEntreTiers;

public class FiscalDataEventNotifierImpl implements FiscalDataEventNotifier {

	private final List<FiscalDataEventListener> listeners;

	public FiscalDataEventNotifierImpl(List<FiscalDataEventListener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void notifyTiersChange(long id) {
		listeners.forEach(listener -> listener.onTiersChange(id));
	}

	@Override
	public void notifyDroitAccessChange(long id) {
		listeners.forEach(listener -> listener.onDroitAccessChange(id));
	}

	@Override
	public void notifyImmeubleChange(long immeubleId) {
		listeners.forEach(listener -> listener.onImmeubleChange(immeubleId));
	}

	@Override
	public void notifyBatimentChange(long batimentId) {
		listeners.forEach(listener -> listener.onBatimentChange(batimentId));
	}

	@Override
	public void notifyCommunauteChange(long communauteId) {
		listeners.forEach(listener -> listener.onCommunauteChange(communauteId));
	}

	@Override
	public void notifyRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		listeners.forEach(listener -> listener.onRelationshipChange(type, sujetId, objetId));
	}

	@Override
	public void notifyLoadDatabase() {
		listeners.forEach(FiscalDataEventListener::onLoadDatabase);
	}

	@Override
	public void notifyTruncateDatabase() {
		listeners.forEach(FiscalDataEventListener::onTruncateDatabase);
	}
}
