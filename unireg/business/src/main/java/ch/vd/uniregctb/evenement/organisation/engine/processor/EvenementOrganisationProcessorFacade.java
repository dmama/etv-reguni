package ch.vd.uniregctb.evenement.organisation.engine.processor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.evenement.organisation.engine.EvenementOrganisationNotificationQueue;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;

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
		stop(false);
	}

	private void stop(boolean aggressiveKill) {
		if (processor != null) {
			if (aggressiveKill) {
				processor.interrupt();
			}
			else {
				processor.stopIt();
			}
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
	public void restartProcessingThread(boolean agressiveKill) {
		// arrêt
		stop(agressiveKill);

		// démarrage
		start();
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setNotificationQueue(EvenementOrganisationNotificationQueue notificationQueue) {
		this.notificationQueue = notificationQueue;
	}

	@Override
	public ListenerHandle registerListener(Listener listener) {
		return publisher.registerListener(listener);
	}

	@Override
	public void unregisterListener(ListenerHandle handle) {
		publisher.unregisterListener(handle);
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
		if (evt.getEtat().isTraite() && evt.getEtat() != EtatEvenementOrganisation.A_VERIFIER) {
			throw new IllegalArgumentException("L'état de l'événement " + evt.getId() + " ne lui permet pas d'être forcé");
		}
		Audit.info(evt.getId(),
		           String.format("Forçage manuel de l'événement organisation %d de type %s au %s sur l'organisation %d",
		                         evt.getId(), evt.getType(), RegDateHelper.dateToDisplayString(evt.getDate()), evt.getNoOrganisation()));
		internalProcessor.processEventForceDoNeutralOnlyOperations(evt);
	}
}
