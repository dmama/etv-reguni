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
		DataEventNotifierHelper.dispatch(listeners, listener -> listener.onTiersChange(id));
	}

	@Override
	public void notifyDroitAccessChange(long id) {
		DataEventNotifierHelper.dispatch(listeners, listener -> listener.onDroitAccessChange(id));
	}

	@Override
	public void notifyImmeubleChange(long immeubleId) {
		DataEventNotifierHelper.dispatch(listeners, listener -> listener.onImmeubleChange(immeubleId));
	}

	@Override
	public void notifyBatimentChange(long batimentId) {
		DataEventNotifierHelper.dispatch(listeners, listener -> listener.onBatimentChange(batimentId));
	}

	@Override
	public void notifyCommunauteChange(long communauteId) {
		DataEventNotifierHelper.dispatch(listeners, listener -> listener.onCommunauteChange(communauteId));
	}

	@Override
	public void notifyRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		DataEventNotifierHelper.dispatch(listeners, listener -> listener.onRelationshipChange(type, sujetId, objetId));
	}

	@Override
	public void notifyLoadDatabase() {
		DataEventNotifierHelper.dispatch(listeners, FiscalDataEventListener::onLoadDatabase);
	}

	@Override
	public void notifyTruncateDatabase() {
		DataEventNotifierHelper.dispatch(listeners, FiscalDataEventListener::onTruncateDatabase);
	}
}
