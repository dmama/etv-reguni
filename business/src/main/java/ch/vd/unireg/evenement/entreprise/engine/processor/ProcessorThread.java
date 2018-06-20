package ch.vd.unireg.evenement.entreprise.engine.processor;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.PollingThread;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.entreprise.engine.EvenementEntrepriseNotificationQueue;

/**
 * Thread de traitement
 *
 */
class ProcessorThread extends PollingThread<EvenementEntrepriseNotificationQueue.Batch> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEntrepriseProcessorFacade.class);

	private final EvenementEntrepriseNotificationQueue eventQueue;

	private final ProcessorPublisher publisher;

	private final ProcessorInternal processor;

	ProcessorThread(EvenementEntrepriseNotificationQueue eventQueue, ProcessorInternal processor, ProcessorPublisher publisher) {
		super("EvtEntreprise");
		this.eventQueue = eventQueue;
		this.processor = processor;
		this.publisher = publisher;
	}

	@Override
	protected EvenementEntrepriseNotificationQueue.Batch poll(@NotNull Duration timeout) throws InterruptedException {
		return eventQueue.poll(timeout);
	}

	@Override
	protected void processElement(@NotNull EvenementEntrepriseNotificationQueue.Batch element) {
		if (element.contenu.size() > 0) {
			processEvents(element.noEntrepriseCivile, element.contenu);
		}
	}

	@Override
	protected void onElementProcessed(@NotNull EvenementEntrepriseNotificationQueue.Batch element, @Nullable Throwable t) {
		super.onElementProcessed(element, t);
		publisher.notifyTraitementEntreprise(element.noEntrepriseCivile);
	}

	@Override
	protected void onStop() {
		super.onStop();
		publisher.notifyStop();
	}

	/**
	 * Prend les événements dans l'ordre et essaie de les traiter. S'arrête à la première erreur.
	 * @param noEntrepriseCivile identifiant de l'individu pour lequel des événements doivent être traités
	 * @param evts descriptifs des événements à traiter
	 */
	private void processEvents(long noEntrepriseCivile, List<EvenementEntrepriseBasicInfo> evts) {
		int pointer = 0;
		final long start = System.nanoTime();
		try {
			LOGGER.info(String.format("Lancement du traitement d'un lot de %d événement(s) pour l'entreprise n°%d", evts.size(), noEntrepriseCivile));
			for (EvenementEntrepriseBasicInfo evt : evts) {
				if (!shouldStop()) {
					if (!processor.processEventAndDoPostProcessingOnError(evt, evts, pointer)) {
						break;
					}
					++ pointer;
				}
			}
		}
		catch (Exception e) {
			LOGGER.error(String.format("Erreur lors du traitement de l'événement entreprise n°%d (rcent: %d)", evts.get(pointer).getId(), evts.get(pointer).getNoEvenement()), e);
		}
		finally {
			final long end = System.nanoTime();
			LOGGER.info(String.format("Lot de %d événement(s) traité en %d ms", evts.size(), TimeUnit.NANOSECONDS.toMillis(end - start)));
		}
	}
}
