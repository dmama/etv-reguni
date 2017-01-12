package ch.vd.uniregctb.data;

public interface DataEventService extends CivilDataEventService, FiscalDataEventService {

	/**
	 * Enregistre un listener à la fois "civil" et "fiscal"
	 *
	 * @param listener le listener à enregistrer
	 */
	default void register(DataEventListener listener) {
		register((CivilDataEventListener) listener);
		register((FiscalDataEventListener) listener);
	}

}
