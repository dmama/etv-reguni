package ch.vd.uniregctb.jmx;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationProcessingMode;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationReceptionMonitor;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessor;

@ManagedResource
public class EvenementsOrganisationJmxBeanImpl implements EvenementsOrganisationJmxBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementsOrganisationJmxBeanImpl.class);

	private EvenementOrganisationReceptionMonitor monitor;

	private EvenementOrganisationProcessor processor;

	@SuppressWarnings("UnusedDeclaration")
	public void setMonitor(EvenementOrganisationReceptionMonitor monitor) {
		this.monitor = monitor;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setProcessor(EvenementOrganisationProcessor processor) {
		this.processor = processor;
	}

	@Override
	@ManagedAttribute
	public int getNbMeaningfullEventsReceived() {
		return monitor.getNombreEvenementsNonIgnores();
	}

	@Override
	@ManagedAttribute
	public int getNbOrganisationsAwaitingTreatment() {
		return monitor.getNombreOrganisationsEnAttenteDeTraitement();
	}

	@Override
	@ManagedAttribute
	public int getNbOrganisationsAwaitingInBatchQueue() {
		return monitor.getNombreOrganisationsEnAttenteDansLaQueueBatch();
	}

	@Override
	@ManagedAttribute
	public int getNbOrganisationsAwaitingInImmediateQueue() {
		return monitor.getNombreOrganisationsEnAttenteDansLaQueueImmediate();
	}

	@Override
	@ManagedAttribute
	public int getNbOrganisationsMovingToFinalQueue() {
		return monitor.getNombreOrganisationsEnTransitionVersLaQueueFinale();
	}

	@Override
	@ManagedAttribute
	public int getNbOrganisationsAwaitingInFinalQueue() {
		return monitor.getNombreOrganisationsEnAttenteDansLaQueueFinale();
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
	public Long getSlidingAverageWaitingTimeInImmediateQueue() {
		return monitor.getMoyenneGlissanteDureeAttenteDansLaQueueImmediate();
	}

	@Override
	public Long getAverageWaitingTimeInImmediateQueue() {
		return monitor.getMoyenneTotaleDureeAttenteDansLaQueueImmediate();
	}

	@Override
	@ManagedOperation
	public void treatOrganisationEvents(long noOrganisation) {
		LOGGER.info("Demande de relance des événements civils de l'organisation " + noOrganisation + " par JMX");
		monitor.demanderTraitementQueue(noOrganisation, EvenementOrganisationProcessingMode.IMMEDIATE);
	}

	@Override
	@ManagedOperation
	public void restartProcessingThread(boolean agressiveKill) {
		LOGGER.info("Demande de redémarrage du thread de traitement des événements oganisation par JMX");
		processor.restartProcessingThread(agressiveKill);
	}
}
