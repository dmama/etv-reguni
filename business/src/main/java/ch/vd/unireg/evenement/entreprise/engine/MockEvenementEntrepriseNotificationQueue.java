package ch.vd.unireg.evenement.entreprise.engine;

import java.time.Duration;
import java.util.Collection;

import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseProcessingMode;

public class MockEvenementEntrepriseNotificationQueue implements EvenementEntrepriseNotificationQueue {

	@Override
	public void post(Long noEntrepriseCivile, EvenementEntrepriseProcessingMode mode) {
		// nothing
	}

	@Override
	public void postAll(Collection<Long> nosEntreprisesCiviles) {
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
	public int getInBulkQueueCount() {
		return 0;
	}

	@Override
	public Long getBulkQueueSlidingAverageAge() {
		return null;
	}

	@Override
	public Long getBulkQueueGlobalAverageAge() {
		return null;
	}

	@Override
	public int getInPriorityQueueCount() {
		return 0;
	}

	@Override
	public Long getPriorityQueueSlidingAverageAge() {
		return null;
	}

	@Override
	public Long getPriorityQueueGlobalAverageAge() {
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
