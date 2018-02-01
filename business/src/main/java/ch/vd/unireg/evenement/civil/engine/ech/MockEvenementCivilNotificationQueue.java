package ch.vd.unireg.evenement.civil.engine.ech;

import java.time.Duration;
import java.util.Collection;

import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchProcessingMode;

public class MockEvenementCivilNotificationQueue implements EvenementCivilNotificationQueue {

	@Override
	public void post(Long noIndividu, EvenementCivilEchProcessingMode mode) {
		// nothing
	}

	@Override
	public void postAll(Collection<Long> nosIndividus) {
		// nothing
	}

	@Override
	public Batch poll(Duration timeout) throws InterruptedException {
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
	public Long getBatchQueueSlidingAverageAge() {
		return null;
	}

	@Override
	public Long getBatchQueueGlobalAverageAge() {
		return null;
	}

	@Override
	public int getInManualQueueCount() {
		return 0;
	}

	@Override
	public Long getManualQueueSlidingAverageAge() {
		return null;
	}

	@Override
	public Long getManualQueueGlobalAverageAge() {
		return null;
	}

	@Override
	public int getInImmediateQueueCount() {
		return 0;
	}

	@Override
	public Long getImmediateQueueSlidingAverageAge() {
		return null;
	}

	@Override
	public Long getImmediateQueueGlobalAverageAge() {
		return null;
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
