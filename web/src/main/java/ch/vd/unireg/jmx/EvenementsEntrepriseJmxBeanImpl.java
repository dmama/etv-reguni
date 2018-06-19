package ch.vd.unireg.jmx;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jmx.export.annotation.ManagedResource;

import ch.vd.unireg.evenement.organisation.EvenementEntrepriseCappingSwitch;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseProcessingMode;
import ch.vd.unireg.evenement.organisation.EvenementEntrepriseReceptionMonitor;
import ch.vd.unireg.evenement.organisation.engine.processor.EvenementEntrepriseProcessor;
import ch.vd.unireg.evenement.organisation.engine.translator.NiveauCappingEtat;

@ManagedResource
public class EvenementsEntrepriseJmxBeanImpl implements EvenementsEntrepriseJmxBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementsEntrepriseJmxBeanImpl.class);

	private EvenementEntrepriseReceptionMonitor monitor;
	private EvenementEntrepriseProcessor processor;
	private EvenementEntrepriseCappingSwitch cappingSwitch;

	@SuppressWarnings("UnusedDeclaration")
	public void setMonitor(EvenementEntrepriseReceptionMonitor monitor) {
		this.monitor = monitor;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setProcessor(EvenementEntrepriseProcessor processor) {
		this.processor = processor;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setCappingSwitch(EvenementEntrepriseCappingSwitch cappingSwitch) {
		this.cappingSwitch = cappingSwitch;
	}

	@Override
	public int getNbMeaningfullEventsReceived() {
		return monitor.getNombreEvenementsNonIgnores();
	}

	@Override
	public int getNbEntreprisesAwaitingTreatment() {
		return monitor.getNombreEntreprisesEnAttenteDeTraitement();
	}

	@Override
	public int getNbEntreprisesAwaitingInBatchQueue() {
		return monitor.getNombreEntreprisesEnAttenteDansLaQueueBatch();
	}

	@Override
	public int getNbEntreprisesAwaitingInPriorityQueue() {
		return monitor.getNombreEntreprisesEnAttenteDansLaQueuePrioritaire();
	}

	@Override
	public int getNbEntreprisesAwaitingInImmediateQueue() {
		return monitor.getNombreEntreprisesEnAttenteDansLaQueueImmediate();
	}

	@Override
	public int getNbEntreprisesMovingToFinalQueue() {
		return monitor.getNombreEntreprisesEnTransitionVersLaQueueFinale();
	}

	@Override
	public int getNbEntreprisesAwaitingInFinalQueue() {
		return monitor.getNombreEntreprisesEnAttenteDansLaQueueFinale();
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
	public void treatEntrepriseEvents(long noEntrepriseCivile) {
		LOGGER.info("Demande de relance des événements civils de l'entreprise " + noEntrepriseCivile + " par JMX");
		monitor.demanderTraitementQueue(noEntrepriseCivile, EvenementEntrepriseProcessingMode.IMMEDIATE);
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
