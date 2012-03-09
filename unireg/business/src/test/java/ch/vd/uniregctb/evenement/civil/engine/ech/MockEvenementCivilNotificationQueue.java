package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class MockEvenementCivilNotificationQueue implements EvenementCivilNotificationQueue {
	@Override
	public void post(Long noIndividu) {
		// nothing
	}

	@Override
	public void postAll(Collection<Long> nosIndividus) {
		// nothing
	}

	@Override
	public Batch poll(long timeout, TimeUnit unit) throws InterruptedException {
		return null;
	}

	@Override
	public int getInflightCount() {
		return 0;
	}
}
