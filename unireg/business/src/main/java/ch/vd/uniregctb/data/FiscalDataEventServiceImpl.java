package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ch.vd.uniregctb.common.LockHelper;
import ch.vd.uniregctb.type.TypeRapportEntreTiers;

/**
 * Partie "fisale" du service de notification des modifications de données
 */
public class FiscalDataEventServiceImpl implements FiscalDataEventService, FiscalDataEventListener {

	private final List<FiscalDataEventListener> listeners = new ArrayList<>();
	private final LockHelper lockHelper = new LockHelper();

	@Override
	public void register(FiscalDataEventListener listener) {
		lockHelper.doInWriteLock(() -> listeners.add(listener));
	}

	@Override
	public void unregister(FiscalDataEventListener listener) {
		lockHelper.doInWriteLock(() -> listeners.removeIf(x -> x == listener));
	}

	private void dispatch(Consumer<? super FiscalDataEventListener> dispatcher) {
		lockHelper.doInReadLock(() -> DataEventServiceHelper.dispatch(listeners, dispatcher));
	}

	@Override
	public void onTiersChange(long id) {
		dispatch(listener -> listener.onTiersChange(id));
	}

	@Override
	public void onDroitAccessChange(long id) {
		dispatch(listener -> listener.onDroitAccessChange(id));
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		dispatch(listener -> listener.onImmeubleChange(immeubleId));
	}

	@Override
	public void onBatimentChange(long batimentId) {
		dispatch(listener -> listener.onBatimentChange(batimentId));
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		dispatch(listener -> listener.onRelationshipChange(type, sujetId, objetId));
	}

	@Override
	public void onLoadDatabase() {
		dispatch(FiscalDataEventListener::onLoadDatabase);
	}

	@Override
	public void onTruncateDatabase() {
		dispatch(FiscalDataEventListener::onTruncateDatabase);
	}
}
