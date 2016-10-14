package ch.vd.uniregctb.evenement.civil.engine.ech;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.NotImplementedException;

public class MockEvenementCivilEvenementProcessor implements EvenementCivilEchProcessor {

	@NotNull
	@Override
	public ListenerHandle registerListener(Listener listener) {
		return () -> {
			// rien à faire...
		};
	}

	@Override
	public void restartProcessingThread(boolean agressiveKill) {
		throw new NotImplementedException();
	}
}
