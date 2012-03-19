package ch.vd.uniregctb.evenement.civil.engine.ech;

import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchProcessor;

public class MockEvenementCivilEvenementProcessor implements EvenementCivilEchProcessor {
	@Override
	public ListenerHandle registerListener(Listener listener) {
		return null;
	}

	@Override
	public void unregisterListener(ListenerHandle handle) {
	}
}
