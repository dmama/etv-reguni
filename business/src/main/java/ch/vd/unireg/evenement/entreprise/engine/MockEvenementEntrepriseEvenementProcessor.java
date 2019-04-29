package ch.vd.unireg.evenement.entreprise.engine;

import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.entreprise.engine.processor.EvenementEntrepriseProcessor;

public class MockEvenementEntrepriseEvenementProcessor implements EvenementEntrepriseProcessor {
	@NotNull
	@Override
	public ListenerHandle registerListener(Listener listener) {
		return () -> {
			// rien à faire de spécial
		};
	}

	@Override
	public void restartProcessingThread() {
		throw new NotImplementedException("");
	}

	@Override
	public void forceEvenement(EvenementEntrepriseBasicInfo evt) {
		throw new NotImplementedException("");
	}
}
