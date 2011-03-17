package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

public class DataEventServiceImpl implements DataEventService {

	private static final Logger LOGGER = Logger.getLogger(DataEventServiceImpl.class);
	
	private final List<DataEventListener> listeners = new ArrayList<DataEventListener>();

	public void register(DataEventListener listener) {
		listeners.add(listener);
	}

	public void onTruncateDatabase() {
		for (int i = 0, listenersSize = listeners.size(); i < listenersSize; i++) { // itération par index pour éviter des problèmes d'accès concurrents lors de l'établissement du context Spring
			final DataEventListener l = listeners.get(i);
			try {
				l.onTruncateDatabase();
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	public void onLoadDatabase() {
		for (int i = 0, listenersSize = listeners.size(); i < listenersSize; i++) {
			final DataEventListener l = listeners.get(i);
			try {
				l.onLoadDatabase();
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	public void onTiersChange(long id) {
		for (int i = 0, listenersSize = listeners.size(); i < listenersSize; i++) {
			final DataEventListener l = listeners.get(i);
			try {
				l.onTiersChange(id);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	public void onIndividuChange(long id) {
		for (int i = 0, listenersSize = listeners.size(); i < listenersSize; i++) {
			final DataEventListener l = listeners.get(i);
			try {
				l.onIndividuChange(id);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	public void onDroitAccessChange(long ppId) {
		for (int i = 0, listenersSize = listeners.size(); i < listenersSize; i++) {
			final DataEventListener l = listeners.get(i);
			try {
				l.onDroitAccessChange(ppId);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}
}
