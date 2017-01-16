package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Partie "civile" du service de notification des modifications de données
 */
public class CivilDataEventServiceImpl implements CivilDataEventService, CivilDataEventListener {

	private final List<CivilDataEventListener> listeners = new ArrayList<>();

	@Override
	public void register(CivilDataEventListener listener) {
		listeners.add(listener);
	}

	@Override
	public void onIndividuChange(long id) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onIndividuChange(id));
	}

	@Override
	public void onOrganisationChange(long id) {
		DataEventServiceHelper.dispatch(listeners, listener -> listener.onOrganisationChange(id));
	}
}
