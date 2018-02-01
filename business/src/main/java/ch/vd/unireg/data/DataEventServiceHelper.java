package ch.vd.unireg.data;

import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Méthodes utilitaires pour la notification de listeners
 */
abstract class DataEventServiceHelper {

	private static final Logger LOGGER = LoggerFactory.getLogger(DataEventServiceHelper.class);

	static <L> void dispatch(List<L> listeners, Consumer<? super L> dispatcher) {
		// itération par index pour éviter des problèmes d'accès concurrents lors de l'établissement du context Spring
		//noinspection ForLoopReplaceableByForEach
		for (int i = 0, listenerSize = listeners.size() ; i < listenerSize ; ++ i) {
			final L listener = listeners.get(i);
			try {
				dispatcher.accept(listener);
			}
			catch (Exception e) {
				LOGGER.error("L'exception ci-après a été ignorée car levée dans un listener", e);
			}
		}
	}

}
