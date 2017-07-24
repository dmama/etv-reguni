package ch.vd.uniregctb.evenement.organisation.engine.processor;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.evenement.organisation.engine.EvenementOrganisationNotificationQueue;

/**
 * Classe de façade du processing des événements organisation reçus de RCEnt
 */
public class EvenementOrganisationProcessorFacade implements EvenementOrganisationProcessor, SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationProcessorFacade.class);

	private EvenementOrganisationNotificationQueue notificationQueue;

	private ProcessorInternal internalProcessor;

	private ProcessorThread processor;

	private final ProcessorPublisher publisher = new ProcessorPublisher();

	@Override
	public void start() {
		if (processor == null) {
			processor = new ProcessorThread(notificationQueue, internalProcessor, publisher);
			processor.setDaemon(false);         // attention, ce thread est très actif en écriture, il ne faudrait pas l'arrêter trop brusquement si possible
			processor.start();
		}
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void stop() {
		if (processor != null) {
			processor.stopIt();
			try {
				processor.join();
			}
			catch (InterruptedException e) {
				// au moins, on aura essayé...
				LOGGER.warn("Attente de terminaison du thread de traitement des événements organisation interrompue", e);
			}
			processor = null;
		}
	}

	@Override
	public void restartProcessingThread() {
		// arrêt
		stop();

		// démarrage
		start();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setNotificationQueue(EvenementOrganisationNotificationQueue notificationQueue) {
		this.notificationQueue = notificationQueue;
	}

	@NotNull
	@Override
	public ListenerHandle registerListener(Listener listener) {
		return publisher.registerListener(listener);
	}

	@Override
	public boolean isRunning() {
		return processor != null && processor.isAlive();
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;   // as late as possible during starting process
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	public ProcessorInternal getInternalProcessor() {
		return internalProcessor;
	}

	public void setInternalProcessor(ProcessorInternal internalProcessor) {
		this.internalProcessor = internalProcessor;
	}

	@Override
	public void forceEvenement(EvenementOrganisationBasicInfo evt) {
		Audit.info(evt.getNoEvenement(),
		           String.format("Forçage manuel de l'événement organisation %s.",
		                         evt.getNoEvenement()));
		internalProcessor.forceEvent(evt);
	}
}
