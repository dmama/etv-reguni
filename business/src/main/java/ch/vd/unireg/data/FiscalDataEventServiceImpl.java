package ch.vd.unireg.data;

import java.util.List;

import ch.vd.unireg.type.TypeRapportEntreTiers;

/**
 * Partie "fisale" du service de notification des modifications de données
 */
public class FiscalDataEventServiceImpl implements FiscalDataEventService, FiscalDataEventListener {

	private final List<FiscalDataEventListener> listeners;

	public FiscalDataEventServiceImpl(List<FiscalDataEventListener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void onTiersChange(long id) {
		listeners.forEach(listener -> listener.onTiersChange(id));
	}

	@Override
	public void onDroitAccessChange(long id) {
		listeners.forEach(listener -> listener.onDroitAccessChange(id));
	}

	@Override
	public void onImmeubleChange(long immeubleId) {
		listeners.forEach(listener -> listener.onImmeubleChange(immeubleId));
	}

	@Override
	public void onBatimentChange(long batimentId) {
		listeners.forEach(listener -> listener.onBatimentChange(batimentId));
	}

	@Override
	public void onCommunauteChange(long communauteId) {
		listeners.forEach(listener -> listener.onCommunauteChange(communauteId));
	}

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		listeners.forEach(listener -> listener.onRelationshipChange(type, sujetId, objetId));
	}

	@Override
	public void onLoadDatabase() {
		listeners.forEach(FiscalDataEventListener::onLoadDatabase);
	}

	@Override
	public void onTruncateDatabase() {
		listeners.forEach(FiscalDataEventListener::onTruncateDatabase);
	}
}
