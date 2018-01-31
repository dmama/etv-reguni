package ch.vd.uniregctb.evenement.organisation.engine;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessor;

public class MockEvenementOrganisationEvenementProcessor implements EvenementOrganisationProcessor {
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
	public void forceEvenement(EvenementOrganisationBasicInfo evt) {
		throw new NotImplementedException();
	}
}
