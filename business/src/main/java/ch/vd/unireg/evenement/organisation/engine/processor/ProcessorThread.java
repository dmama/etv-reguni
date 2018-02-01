package ch.vd.unireg.evenement.organisation.engine.processor;

/**
 * @author Raphaël Marmier, 2015-07-27
 */

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.vd.unireg.common.PollingThread;
import ch.vd.unireg.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.unireg.evenement.organisation.engine.EvenementOrganisationNotificationQueue;

/**
 * Thread de traitement
 *
 */
class ProcessorThread extends PollingThread<EvenementOrganisationNotificationQueue.Batch> {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationProcessorFacade.class);

	private final EvenementOrganisationNotificationQueue eventQueue;

	private final ProcessorPublisher publisher;

	private final ProcessorInternal processor;

	ProcessorThread(EvenementOrganisationNotificationQueue eventQueue, ProcessorInternal processor, ProcessorPublisher publisher) {
		super("EvtOrganisation");
		this.eventQueue = eventQueue;
		this.processor = processor;
		this.publisher = publisher;
	}

	@Override
	protected EvenementOrganisationNotificationQueue.Batch poll(@NotNull Duration timeout) throws InterruptedException {
		return eventQueue.poll(timeout);
	}

	@Override
	protected void processElement(@NotNull EvenementOrganisationNotificationQueue.Batch element) {
		if (element.contenu.size() > 0) {
			processEvents(element.noOrganisation, element.contenu);
		}
	}

	@Override
	protected void onElementProcessed(@NotNull EvenementOrganisationNotificationQueue.Batch element, @Nullable Throwable t) {
		super.onElementProcessed(element, t);
		publisher.notifyTraitementOrganisation(element.noOrganisation);
	}

	@Override
	protected void onStop() {
		super.onStop();
		publisher.notifyStop();
	}

	/**
	 * Prend les événements dans l'ordre et essaie de les traiter. S'arrête à la première erreur.
	 * @param noOrganisation identifiant de l'individu pour lequel des événements doivent être traités
	 * @param evts descriptifs des événements à traiter
	 */
	private void processEvents(long noOrganisation, List<EvenementOrganisationBasicInfo> evts) {
		int pointer = 0;
		final long start = System.nanoTime();
		try {
			LOGGER.info(String.format("Lancement du traitement d'un lot de %d événement(s) pour l'organisation n°%d", evts.size(), noOrganisation));
			for (EvenementOrganisationBasicInfo evt : evts) {
				if (!shouldStop()) {
					if (!processor.processEventAndDoPostProcessingOnError(evt, evts, pointer)) {
						break;
					}
					++ pointer;
				}
			}
		}
		catch (Exception e) {
			LOGGER.error(String.format("Erreur lors du traitement de l'événement organisation n°%d (rcent: %d)", evts.get(pointer).getId(), evts.get(pointer).getNoEvenement()), e);
		}
		finally {
			final long end = System.nanoTime();
			LOGGER.info(String.format("Lot de %d événement(s) traité en %d ms", evts.size(), TimeUnit.NANOSECONDS.toMillis(end - start)));
		}
	}
}
