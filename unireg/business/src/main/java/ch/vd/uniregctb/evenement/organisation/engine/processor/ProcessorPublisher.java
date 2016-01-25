package ch.vd.uniregctb.evenement.organisation.engine.processor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Raphaël Marmier, 2015-07-27
 */
public class ProcessorPublisher {

	private final Map<Long, EvenementOrganisationProcessor.Listener> listeners = new LinkedHashMap<>();      // pour les tests, c'est pratique de conserver l'ordre (pour le reste, cela ne fait pas de mal...)

	private interface Sequencer {
		long next();
	}

	private static final Sequencer SEQUENCER = new Sequencer() {
		private final AtomicLong sequenceNumber = new AtomicLong(0L);
		public long next() {
			return sequenceNumber.getAndIncrement();
		}
	};

	/**
	 * Classe interne des handles utilisés lors de l'enregistrement de listeners
	 */
	private final class ListenerHandleImpl implements EvenementOrganisationProcessor.ListenerHandle {
		private final long id;
		private ListenerHandleImpl(long id) {
			this.id = id;
		}
	}

	/**
	 * Appelé par le thread de traitement à chaque fois que les événements organisation d'une organisation ont été traités
	 * @param noOrganisation numéro de l'organisation dont les événements viennent d'être traités
	 */
	void notifyTraitementOrganisation(long noOrganisation) {
		synchronized (listeners) {
			if (listeners.size() > 0) {
				for (EvenementOrganisationProcessor.Listener listener : listeners.values()) {
					try {
						listener.onOrganisationTraite(noOrganisation);
					}
					catch (Exception e) {
						// pas grave...
					}
				}
			}
		}
	}

	/**
	 * Appelé par le thread de traitement juste avant de s'arrêter
	 */
	void notifyStop() {
		synchronized (listeners) {
			if (listeners.size() > 0) {
				for (EvenementOrganisationProcessor.Listener listener : listeners.values()) {
					try {
						listener.onStop();
					}
					catch (Exception e) {
						// pas grave...
					}
				}
			}
		}
	}

	protected EvenementOrganisationProcessor.ListenerHandle registerListener(EvenementOrganisationProcessor.Listener listener) {
		if (listener == null) {
			throw new NullPointerException("listener");
		}
		final long id = SEQUENCER.next();
		synchronized (listeners) {
			listeners.put(id, listener);
		}
		return new ListenerHandleImpl(id);
	}

	protected void unregisterListener(EvenementOrganisationProcessor.ListenerHandle handle) {
		if (!(handle instanceof ListenerHandleImpl)) {
			throw new IllegalArgumentException("Invalid handle");
		}
		synchronized (listeners) {
			listeners.remove(((ListenerHandleImpl) handle).id);
		}
	}


}
