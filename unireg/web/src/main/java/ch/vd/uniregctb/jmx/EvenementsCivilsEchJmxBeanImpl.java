package ch.vd.uniregctb.jmx;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchReceptionMonitor;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilEchProcessor;
import ch.vd.uniregctb.jms.ErrorMonitorableMessageListener;
import ch.vd.uniregctb.jms.JmxAwareEsbMessageEndpointManager;

import static ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchReceptionHandler.Mode;

@ManagedResource
public class EvenementsCivilsEchJmxBeanImpl implements EvenementsCivilsEchJmxBean, InitializingBean {

	private static final Logger LOGGER = Logger.getLogger(EvenementsCivilsEchJmxBeanImpl.class);

	private JmxAwareEsbMessageEndpointManager endpointManagerMasse;
	private JmxAwareEsbMessageEndpointManager endpointManagerIndividuel;
	private EvenementCivilEchReceptionMonitor monitor;

	private ErrorMonitorableMessageListener masseListener;
	private ErrorMonitorableMessageListener individuelListener;

	private EvenementCivilEchProcessor processor;

	@SuppressWarnings("UnusedDeclaration")
	public void setMonitor(EvenementCivilEchReceptionMonitor monitor) {
		this.monitor = monitor;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setEndpointManagerMasse(JmxAwareEsbMessageEndpointManager endpointManagerMasse) {
		this.endpointManagerMasse = endpointManagerMasse;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setEndpointManagerIndividuel(JmxAwareEsbMessageEndpointManager endpointManagerIndividuel) {
		this.endpointManagerIndividuel = endpointManagerIndividuel;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setProcessor(EvenementCivilEchProcessor processor) {
		this.processor = processor;
	}

	@Override
	@ManagedAttribute
	public int getNbMeaningfullEventsReceived() {
		return monitor.getNombreEvenementsNonIgnores();
	}

	@Override
	@ManagedAttribute
	public int getNbIndividualsAwaitingTreatment() {
		return monitor.getNombreIndividusEnAttenteDeTraitement();
	}

	@Override
	@ManagedAttribute
	public int getNbIndividualsAwaitingInBatchQueue() {
		return monitor.getNombreIndividusEnAttenteDansLaQueueBatch();
	}

	@Override
	@ManagedAttribute
	public int getNbIndividualsAwaitingInManualQueue() {
		return monitor.getNombreIndividusEnAttenteDansLaQueueManuelle();
	}

	@Override
	@ManagedAttribute
	public int getNbIndividualsMovingToFinalQueue() {
		return monitor.getNombreIndividusEnTransitionVersLaQueueFinale();
	}

	@Override
	@ManagedAttribute
	public int getNbIndividualsAwaitingInFinalQueue() {
		return monitor.getNombreIndividusEnAttenteDansLaQueueFinale();
	}

	@Override
	@ManagedAttribute
	public int getNbManualEventsReceived() {
		return individuelListener.getNombreMessagesRecus();
	}

	@Override
	@ManagedAttribute
	public int getNbBatchEventsReceived() {
		return masseListener.getNombreMessagesRecus();
	}

	@Override
	@ManagedAttribute
	public int getNbManualEventsRejectedToErrorQueue() {
		return individuelListener.getNombreMessagesRenvoyesEnErreur();
	}

	@Override
	@ManagedAttribute
	public int getNbBatchEventsRejectedToErrorQueue() {
		return masseListener.getNombreMessagesRenvoyesEnErreur();
	}

	@Override
	@ManagedAttribute
	public int getNbManualEventsRejectedException() {
		return individuelListener.getNombreMessagesRenvoyesEnException();
	}

	@Override
	@ManagedAttribute
	public int getNbBatchEventsRejectedException() {
		return masseListener.getNombreMessagesRenvoyesEnException();
	}

	@Override
	@ManagedAttribute
	public int getNbManualConsumers() {
		return endpointManagerIndividuel.getMaxConcurrentConsumers();
	}

	@Override
	@ManagedAttribute
	public int getNbBatchConsumers() {
		return endpointManagerMasse.getMaxConcurrentConsumers();
	}

	@Override
	@ManagedOperation
	public void treatPersonsEvents(long noIndividu) {
		LOGGER.info("Demande de relance des événements civils de l'individu " + noIndividu + " par JMX");
		monitor.demanderTraitementQueue(noIndividu, true, Mode.MANUAL);
	}

	@Override
	@ManagedOperation
	public void restartProcessingThread(boolean agressiveKill) {
		LOGGER.info("Demande de redémarrage du thread de traitement des événements civils par JMX");
		processor.restartProcessingThread(agressiveKill);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (endpointManagerIndividuel == null) {
			throw new IllegalArgumentException("La propriété endpointManagerIndividuel est nulle");
		}
		else {
			final Object listener = endpointManagerIndividuel.getMessageListener();
			if (listener == null || !(listener instanceof ErrorMonitorableMessageListener)) {
				throw new IllegalArgumentException("Le listener d'événements civils doit implémenter l'interface " + ErrorMonitorableMessageListener.class.getName());
			}

			this.individuelListener = (ErrorMonitorableMessageListener) listener;
		}

		if (endpointManagerMasse == null) {
			throw new IllegalArgumentException("La propriété endpointManagerMasse est nulle");
		}
		else {
			final Object listener = endpointManagerMasse.getMessageListener();
			if (listener == null || !(listener instanceof ErrorMonitorableMessageListener)) {
				throw new IllegalArgumentException("Le listener d'événements civils doit implémenter l'interface " + ErrorMonitorableMessageListener.class.getName());
			}

			this.masseListener = (ErrorMonitorableMessageListener) listener;
		}

		if (this.masseListener == this.individuelListener) {
			throw new IllegalArgumentException("Les deux listeners 'masse' et 'individuels' devraient être différents");
		}
	}
}
