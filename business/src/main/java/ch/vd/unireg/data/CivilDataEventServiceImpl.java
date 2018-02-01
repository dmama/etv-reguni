package ch.vd.unireg.data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import ch.vd.unireg.common.LockHelper;

/**
 * Partie "civile" du service de notification des modifications de données
 */
public class CivilDataEventServiceImpl implements CivilDataEventService, CivilDataEventListener {

	private final List<CivilDataEventListener> listeners = new ArrayList<>();
	private final LockHelper lockHelper = new LockHelper();

	@Override
	public void register(CivilDataEventListener listener) {
		lockHelper.doInWriteLock(() -> listeners.add(listener));
	}

	@Override
	public void unregister(CivilDataEventListener listener) {
		lockHelper.doInWriteLock(() -> listeners.removeIf(x -> x == listener));
	}

	private void dispatch(Consumer<? super CivilDataEventListener> dispatcher) {
		lockHelper.doInReadLock(() -> DataEventServiceHelper.dispatch(listeners, dispatcher));
	}

	@Override
	public void onIndividuChange(long id) {
		dispatch(listener -> listener.onIndividuChange(id));
	}

	@Override
	public void onOrganisationChange(long id) {
		dispatch(listener -> listener.onOrganisationChange(id));
	}
}
