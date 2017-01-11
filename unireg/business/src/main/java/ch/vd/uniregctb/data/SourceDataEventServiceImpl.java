package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;

/**
 * Partie "source" du service de notification des modifications de données
 */
public class SourceDataEventServiceImpl implements SourceDataEventService {

	private final List<SourceDataEventListener> listeners = new ArrayList<>();

	@Override
	public void register(SourceDataEventListener listener) {
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
