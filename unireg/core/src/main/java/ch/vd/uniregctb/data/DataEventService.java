package ch.vd.uniregctb.data;

public interface DataEventService extends SourceDataEventService, SinkDataEventService {

	/**
	 * Enregistre un listener à la fois "source" et "sink"
	 *
	 * @param listener le listener à enregistrer
	 */
	default void register(DataEventListener listener) {
		register((SourceDataEventListener) listener);
		register((SinkDataEventListener) listener);
	}

}
