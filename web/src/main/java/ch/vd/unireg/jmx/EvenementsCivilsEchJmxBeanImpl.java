package ch.vd.unireg.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchProcessingMode;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchReceptionMonitor;
import ch.vd.unireg.evenement.civil.engine.ech.EvenementCivilEchProcessor;

@ManagedResource
public class EvenementsCivilsEchJmxBeanImpl implements EvenementsCivilsEchJmxBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementsCivilsEchJmxBeanImpl.class);

	private EvenementCivilEchReceptionMonitor monitor;

	private EvenementCivilEchProcessor processor;

	@SuppressWarnings("UnusedDeclaration")
	public void setMonitor(EvenementCivilEchReceptionMonitor monitor) {
		this.monitor = monitor;
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
	public int getNbIndividualsAwaitingInImmediateQueue() {
		return monitor.getNombreIndividusEnAttenteDansLaQueueImmediate();
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
	public Long getSlidingAverageWaitingTimeInBatchQueue() {
		return monitor.getMoyenneGlissanteDureeAttenteDansLaQueueBatch();
	}

	@Override
	public Long getAverageWaitingTimeInBatchQueue() {
		return monitor.getMoyenneTotaleDureeAttenteDansLaQueueBatch();
	}

	@Override
	public Long getSlidingAverageWaitingTimeInManualQueue() {
		return monitor.getMoyenneGlissanteDureeAttenteDansLaQueueManuelle();
	}

	@Override
	public Long getAverageWaitingTimeInManualQueue() {
		return monitor.getMoyenneTotaleDureeAttenteDansLaQueueManuelle();
	}

	@Override
	public Long getSlidingAverageWaitingTimeInImmediateQueue() {
		return monitor.getMoyenneGlissanteDureeAttenteDansLaQueueImmediate();
	}

	@Override
	public Long getAverageWaitingTimeInImmediateQueue() {
		return monitor.getMoyenneTotaleDureeAttenteDansLaQueueImmediate();
	}

	@Override
	@ManagedOperation
	public void treatPersonsEvents(long noIndividu) {
		LOGGER.info("Demande de relance des événements civils de l'individu " + noIndividu + " par JMX");
		monitor.demanderTraitementQueue(noIndividu, EvenementCivilEchProcessingMode.IMMEDIATE);
	}

	@Override
	@ManagedOperation
	public void restartProcessingThread() {
		LOGGER.info("Demande de redémarrage du thread de traitement des événements civils par JMX");
		processor.restartProcessingThread();
	}
}
