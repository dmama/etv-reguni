package ch.vd.uniregctb.evenement.civil.engine.ech;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockEvenementCivilEvenementProcessor implements EvenementCivilEchProcessor {
	@Override
	public ListenerHandle registerListener(Listener listener) {
		return null;
	}

	@Override
	public void unregisterListener(ListenerHandle handle) {
	}

	@Override
	public void restartProcessingThread(boolean agressiveKill) {
		throw new NotImplementedException();
	}
}
