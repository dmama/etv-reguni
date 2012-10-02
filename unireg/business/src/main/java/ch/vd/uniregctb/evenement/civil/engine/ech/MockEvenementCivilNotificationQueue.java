package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

public class MockEvenementCivilNotificationQueue implements EvenementCivilNotificationQueue {

	@Override
	public void postBatch(Long noIndividu, boolean immediate) {
		// nothing
	}

	@Override
	public void postManual(Long noIndividu, boolean immediate) {
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
	public int getTotalCount() {
		return 0;
	}

	@Override
	public int getInBatchQueueCount() {
		return 0;
	}

	@Override
	public int getInManualQueueCount() {
		return 0;
	}

	@Override
	public int getInFinalQueueCount() {
		return 0;
	}

	@Override
	public int getInHatchesCount() {
		return 0;
	}

}
