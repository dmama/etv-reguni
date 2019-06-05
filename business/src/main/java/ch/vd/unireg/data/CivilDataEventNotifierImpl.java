package ch.vd.unireg.data;

import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Service de notification des modifications sur les données civiles.
 */
public class CivilDataEventNotifierImpl implements CivilDataEventNotifier {

	private final List<CivilDataEventListener> listeners;

	public CivilDataEventNotifierImpl(@NotNull List<CivilDataEventListener> listeners) {
		this.listeners = listeners;
	}

	@Override
	public void notifyIndividuChange(long id) {
		DataEventNotifierHelper.dispatch(listeners, listener -> listener.onIndividuChange(id));
	}

	@Override
	public void notifyEntrepriseChange(long id) {
		DataEventNotifierHelper.dispatch(listeners, listener -> listener.onEntrepriseChange(id));
	}
}
