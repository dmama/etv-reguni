package ch.vd.uniregctb.jmx;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationCappingSwitch;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationProcessingMode;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationReceptionMonitor;
import ch.vd.uniregctb.evenement.organisation.engine.processor.EvenementOrganisationProcessor;
import ch.vd.uniregctb.evenement.organisation.engine.translator.NiveauCappingEtat;

@ManagedResource
public class EvenementsOrganisationJmxBeanImpl implements EvenementsOrganisationJmxBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementsOrganisationJmxBeanImpl.class);

	private EvenementOrganisationReceptionMonitor monitor;
	private EvenementOrganisationProcessor processor;
	private EvenementOrganisationCappingSwitch cappingSwitch;

	@SuppressWarnings("UnusedDeclaration")
	public void setMonitor(EvenementOrganisationReceptionMonitor monitor) {
		this.monitor = monitor;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setProcessor(EvenementOrganisationProcessor processor) {
		this.processor = processor;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setCappingSwitch(EvenementOrganisationCappingSwitch cappingSwitch) {
		this.cappingSwitch = cappingSwitch;
	}

	@Override
	public int getNbMeaningfullEventsReceived() {
		return monitor.getNombreEvenementsNonIgnores();
	}

	@Override
	public int getNbOrganisationsAwaitingTreatment() {
		return monitor.getNombreOrganisationsEnAttenteDeTraitement();
	}

	@Override
	public int getNbOrganisationsAwaitingInBatchQueue() {
		return monitor.getNombreOrganisationsEnAttenteDansLaQueueBatch();
	}

	@Override
	public int getNbOrganisationsAwaitingInPriorityQueue() {
		return monitor.getNombreOrganisationsEnAttenteDansLaQueuePrioritaire();
	}

	@Override
	public int getNbOrganisationsAwaitingInImmediateQueue() {
		return monitor.getNombreOrganisationsEnAttenteDansLaQueueImmediate();
	}

	@Override
	public int getNbOrganisationsMovingToFinalQueue() {
		return monitor.getNombreOrganisationsEnTransitionVersLaQueueFinale();
	}

	@Override
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
	public Long getSlidingAverageWaitingTimeInPriorityQueue() {
		return monitor.getMoyenneGlissanteDureeAttenteDansLaQueuePrioritaire();
	}

	@Override
	public Long getAverageWaitingTimeInPriorityQueue() {
		return monitor.getMoyenneTotaleDureeAttenteDansLaQueuePrioritaire();
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
	public void treatOrganisationEvents(long noOrganisation) {
		LOGGER.info("Demande de relance des événements civils de l'organisation " + noOrganisation + " par JMX");
		monitor.demanderTraitementQueue(noOrganisation, EvenementOrganisationProcessingMode.IMMEDIATE);
	}

	@Override
	public void restartProcessingThread() {
		LOGGER.info("Demande de redémarrage du thread de traitement des événements oganisation par JMX");
		processor.restartProcessingThread();
	}

	@Override
	public String getProcessingCappingLevel() {
		final NiveauCappingEtat niveau = cappingSwitch.getNiveauCapping();
		return niveau != null ? niveau.name() : null;
	}

	@Override
	public void setProcessingCappingLevel(String level) {
		final NiveauCappingEtat niveau;
		if (StringUtils.isBlank(level)) {
			niveau = null;
		}
		else {
			niveau = NiveauCappingEtat.valueOf(level);
		}
		cappingSwitch.setNiveauCapping(niveau);
	}
}
