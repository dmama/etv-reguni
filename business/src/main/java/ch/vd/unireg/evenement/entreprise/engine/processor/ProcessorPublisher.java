package ch.vd.unireg.evenement.entreprise.engine.processor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.jetbrains.annotations.NotNull;

/**
 * @author Raphaël Marmier, 2015-07-27
 */
public class ProcessorPublisher {

	private final Map<Long, EvenementEntrepriseProcessor.Listener> listeners = new LinkedHashMap<>();      // pour les tests, c'est pratique de conserver l'ordre (pour le reste, cela ne fait pas de mal...)

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
	private final class ListenerHandleImpl implements EvenementEntrepriseProcessor.ListenerHandle {
		private final long id;
		private ListenerHandleImpl(long id) {
			this.id = id;
		}

		@Override
		public void unregister() {
			synchronized (listeners) {
				if (listeners.remove(id) == null) {
					throw new IllegalStateException("Already unregistered!");
				}
			}
		}
	}

	/**
	 * Appelé par le thread de traitement à chaque fois que les événements entreprise d'une entreprise ont été traités
	 * @param noEntrepriseCivile numéro de l'entreprise dont les événements viennent d'être traités
	 */
	void notifyTraitementEntreprise(long noEntrepriseCivile) {
		synchronized (listeners) {
			if (listeners.size() > 0) {
				for (EvenementEntrepriseProcessor.Listener listener : listeners.values()) {
					try {
						listener.onEntrepriseTraitee(noEntrepriseCivile);
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
				for (EvenementEntrepriseProcessor.Listener listener : listeners.values()) {
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

	@NotNull
	protected EvenementEntrepriseProcessor.ListenerHandle registerListener(EvenementEntrepriseProcessor.Listener listener) {
		if (listener == null) {
			throw new NullPointerException("listener");
		}
		final long id = SEQUENCER.next();
		synchronized (listeners) {
			listeners.put(id, listener);
		}
		return new ListenerHandleImpl(id);
	}
}
