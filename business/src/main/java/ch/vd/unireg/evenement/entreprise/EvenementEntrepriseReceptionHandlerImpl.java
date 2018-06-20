package ch.vd.unireg.evenement.entreprise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.audit.Audit;
import ch.vd.unireg.evenement.entreprise.engine.EvenementEntrepriseNotificationQueue;
import ch.vd.unireg.load.BasicLoadMonitor;
import ch.vd.unireg.load.LoadAverager;
import ch.vd.unireg.stats.LoadMonitorable;
import ch.vd.unireg.stats.StatsService;

public class EvenementEntrepriseReceptionHandlerImpl implements EvenementEntrepriseReceptionHandler, EvenementEntrepriseReceptionMonitor, InitializingBean, DisposableBean {

	private EvenementEntrepriseNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementEntrepriseDAO evtEntrepriseDAO;

	private static final String SERVICE_NAME = "EvtsEntrepriseQueueSize";
	private StatsService statsService;
	private LoadAverager loadAverager;

	private final AtomicInteger nombreEvenementsNonIgnores = new AtomicInteger(0);

    public EvenementEntrepriseReceptionHandlerImpl() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
	public void setNotificationQueue(EvenementEntrepriseNotificationQueue notificationQueue) {
		this.notificationQueue = notificationQueue;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtEntrepriseDAO(EvenementEntrepriseDAO evtEntrepriseDAO) {
		this.evtEntrepriseDAO = evtEntrepriseDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	@Override
	public int getNombreEvenementsNonIgnores() {
		return nombreEvenementsNonIgnores.intValue();
	}

	@Override
	public int getNombreEntreprisesEnAttenteDeTraitement() {
		return notificationQueue.getTotalCount();
	}

	@Override
	public int getNombreEntreprisesEnAttenteDansLaQueueBatch() {
		return notificationQueue.getInBulkQueueCount();
	}

	@Override
	public int getNombreEntreprisesEnAttenteDansLaQueuePrioritaire() {
		return notificationQueue.getInPriorityQueueCount();
	}

	@Override
	public Long getMoyenneGlissanteDureeAttenteDansLaQueueBatch() {
		return notificationQueue.getBulkQueueSlidingAverageAge();
	}

	@Override
	public Long getMoyenneGlissanteDureeAttenteDansLaQueuePrioritaire() {
		return notificationQueue.getPriorityQueueSlidingAverageAge();
	}

	@Override
	public Long getMoyenneTotaleDureeAttenteDansLaQueueBatch() {
		return notificationQueue.getBulkQueueGlobalAverageAge();
	}

	@Override
	public Long getMoyenneTotaleDureeAttenteDansLaQueuePrioritaire() {
		return notificationQueue.getPriorityQueueGlobalAverageAge();
	}

	@Override
	public int getNombreEntreprisesEnAttenteDansLaQueueImmediate() {
		return notificationQueue.getInImmediateQueueCount();
	}

	@Override
	public Long getMoyenneGlissanteDureeAttenteDansLaQueueImmediate() {
		return notificationQueue.getImmediateQueueSlidingAverageAge();
	}

	@Override
	public Long getMoyenneTotaleDureeAttenteDansLaQueueImmediate() {
		return notificationQueue.getImmediateQueueGlobalAverageAge();
	}

	@Override
	public int getNombreEntreprisesEnTransitionVersLaQueueFinale() {
		return notificationQueue.getInHatchesCount();
	}

	@Override
	public int getNombreEntreprisesEnAttenteDansLaQueueFinale() {
		return notificationQueue.getInFinalQueueCount();
	}

	@Override
	public void demanderTraitementQueue(long noEntrepriseCivile, EvenementEntrepriseProcessingMode mode) {
		notificationQueue.post(noEntrepriseCivile, mode);
	}


	/**
	 * Contrôle si un événement RCEnt a déjà été receptionné pour le businessId.
	 *
	 * @param businessId le businessId de l'événement à contrôler
	 * @return <code>true</code> si l'événement correspondant au businessId a déjà été reçu. <code>false</code> sinon.
	 */
	@Override
	public boolean dejaRecu(String businessId) {
		return !evtEntrepriseDAO.getEvenementsForBusinessId(businessId).isEmpty();
	}

	/**
	 * Sauve les événements issus d'un événement entreprise.
	 *
 	 * @param events événements à persister
	 * @return la liste des événements sauvés
	 */
	@Override
    @NotNull
	public List<EvenementEntreprise> saveIncomingEvent(final List<EvenementEntreprise> events) {
		if(events.isEmpty()) {
			throw new IllegalArgumentException();
		}

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<List<EvenementEntreprise>>() {
			@NotNull
			@Override
			public List<EvenementEntreprise> doInTransaction(TransactionStatus status) {
				List<EvenementEntreprise> saved = new ArrayList<>();

				// pour les stats
				nombreEvenementsNonIgnores.incrementAndGet();

				for (EvenementEntreprise event : events) {
					final EvenementEntreprise savedEvent = evtEntrepriseDAO.save(event);
					saved.add(savedEvent);
					Audit.info(event.getNoEvenement(), String.format("L'événement entreprise %d pour l'entreprise %d est inséré en base de données", event.getNoEvenement(), event.getNoEntrepriseCivile()));
				}
				return saved;
			}
		});
	}

	@Override
	public List<EvenementEntreprise> handleEvents(List<EvenementEntreprise> events, EvenementEntrepriseProcessingMode mode) throws EvenementEntrepriseException {
		for (EvenementEntreprise event : events) {
			demanderTraitementQueue(event.getNoEntrepriseCivile(), mode);
		}
		return events;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {

			// façade de monitoring sur la queue d'attente de traitement des événements entreprise
			// où la charge est définie comme le nombre d'entreprises en attente de traitement
			final LoadMonitorable service = this::getNombreEntreprisesEnAttenteDeTraitement;

			// calculateur de moyenne de charge sur les 5 dernières minutes (échantillonnage à 2 fois par seconde)
			loadAverager = new LoadAverager(service, SERVICE_NAME, 600, 500);
			loadAverager.start();

			// enregistrement du monitoring
			statsService.registerLoadMonitor(SERVICE_NAME, new BasicLoadMonitor(service, loadAverager));
		}
	}

	@Override
	public void destroy() throws Exception {
		if (loadAverager != null) {
			loadAverager.stop();
			loadAverager = null;
		}
		if (statsService != null) {
			statsService.unregisterLoadMonitor(SERVICE_NAME);
		}
	}
}
