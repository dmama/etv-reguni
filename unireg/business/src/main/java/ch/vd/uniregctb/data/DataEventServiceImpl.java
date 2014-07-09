package ch.vd.uniregctb.data;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class DataEventServiceImpl implements DataEventService {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataEventServiceImpl.class);
	
	private final List<DataEventListener> listeners = new ArrayList<>();

	@Override
	public void register(DataEventListener listener) {
		listeners.add(listener);
	}

	@Override
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

	@Override
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

	@Override
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

	@Override
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

	@Override
	public void onPersonneMoraleChange(long id) {
		for (int i = 0, listenersSize = listeners.size(); i < listenersSize; i++) {
			final DataEventListener l = listeners.get(i);
			try {
				l.onPersonneMoraleChange(id);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

	@Override
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

	@Override
	public void onRelationshipChange(TypeRapportEntreTiers type, long sujetId, long objetId) {
		for (int i = 0, listenersSize = listeners.size(); i < listenersSize; i++) {
			final DataEventListener l = listeners.get(i);
			try {
				l.onRelationshipChange(type, sujetId, objetId);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}
}
