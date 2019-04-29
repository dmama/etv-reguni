package ch.vd.unireg.evenement.civil.engine.ech;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

public class MockEvenementCivilEvenementProcessor implements EvenementCivilEchProcessor {

	@NotNull
	@Override
	public ListenerHandle registerListener(Listener listener) {
		return () -> {
			// rien à faire...
		};
	}

	@Override
	public void restartProcessingThread() {
		throw new NotImplementedException("");
	}
}
