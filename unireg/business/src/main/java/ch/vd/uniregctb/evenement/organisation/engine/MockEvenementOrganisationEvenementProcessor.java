package ch.vd.uniregctb.evenement.organisation.engine;

import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessor;

public class MockEvenementOrganisationEvenementProcessor implements EvenementOrganisationProcessor {
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

	@Override
	public void forceEvenement(EvenementOrganisationBasicInfo evt) {
		throw new NotImplementedException();
	}
}
