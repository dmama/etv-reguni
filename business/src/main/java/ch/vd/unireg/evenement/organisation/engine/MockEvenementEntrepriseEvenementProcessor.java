package ch.vd.unireg.evenement.organisation.engine;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.organisation.engine.processor.EvenementEntrepriseProcessor;

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
		throw new NotImplementedException();
	}

	@Override
	public void forceEvenement(EvenementEntrepriseBasicInfo evt) {
		throw new NotImplementedException();
	}
}
