package ch.vd.unireg.evenement.organisation.engine.processor;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.organisation.engine.EvenementEntrepriseNotificationQueue;

/**
 * Classe de façade du processing des événements entreprise reçus de RCEnt
 */
public class EvenementEntrepriseProcessorFacade implements EvenementEntrepriseProcessor, SmartLifecycle {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEntrepriseProcessorFacade.class);

	private EvenementEntrepriseNotificationQueue notificationQueue;

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
				LOGGER.warn("Attente de terminaison du thread de traitement des événements entreprise interrompue", e);
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
	public void setNotificationQueue(EvenementEntrepriseNotificationQueue notificationQueue) {
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
	public void forceEvenement(EvenementEntrepriseBasicInfo evt) {
		Audit.info(evt.getNoEvenement(),
		           String.format("Forçage manuel de l'événement entreprise %s.",
		                         evt.getNoEvenement()));
		internalProcessor.forceEvent(evt);
	}
}
