package ch.vd.unireg.data;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Partie "civile" du service de notification des modifications de données
 */
public class CivilDataEventServiceImpl implements CivilDataEventService, CivilDataEventListener {

	private final List<CivilDataEventListener> listeners;

	public CivilDataEventServiceImpl(@NotNull List<CivilDataEventListener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void onIndividuChange(long id) {
		listeners.forEach(listener -> listener.onIndividuChange(id));
	}

	@Override
	public void onEntrepriseChange(long id) {
		listeners.forEach(listener -> listener.onEntrepriseChange(id));
	}
}
