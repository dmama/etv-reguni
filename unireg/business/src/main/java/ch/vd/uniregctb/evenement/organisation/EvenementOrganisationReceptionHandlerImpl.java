package ch.vd.uniregctb.evenement.organisation;

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

import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.engine.EvenementOrganisationNotificationQueue;
import ch.vd.uniregctb.load.BasicLoadMonitor;
import ch.vd.uniregctb.load.LoadAverager;
import ch.vd.uniregctb.stats.LoadMonitorable;
import ch.vd.uniregctb.stats.StatsService;
import ch.vd.uniregctb.transaction.TransactionTemplate;

public class EvenementOrganisationReceptionHandlerImpl implements EvenementOrganisationReceptionHandler, EvenementOrganisationReceptionMonitor, InitializingBean, DisposableBean {

	private EvenementOrganisationNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementOrganisationDAO evtOrganisationDAO;

	private static final String SERVICE_NAME = "EvtsOrganisationQueueSize";
	private StatsService statsService;
	private LoadAverager loadAverager;

	private final AtomicInteger nombreEvenementsNonIgnores = new AtomicInteger(0);

    public EvenementOrganisationReceptionHandlerImpl() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
	public void setNotificationQueue(EvenementOrganisationNotificationQueue notificationQueue) {
		this.notificationQueue = notificationQueue;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtOrganisationDAO(EvenementOrganisationDAO evtOrganisationDAO) {
		this.evtOrganisationDAO = evtOrganisationDAO;
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
	public int getNombreOrganisationsEnAttenteDeTraitement() {
		return notificationQueue.getTotalCount();
	}

	@Override
	public int getNombreOrganisationsEnAttenteDansLaQueueBatch() {
		return notificationQueue.getInBulkQueueCount();
	}

	@Override
	public int getNombreOrganisationsEnAttenteDansLaQueuePrioritaire() {
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
	public int getNombreOrganisationsEnAttenteDansLaQueueImmediate() {
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
	public int getNombreOrganisationsEnTransitionVersLaQueueFinale() {
		return notificationQueue.getInHatchesCount();
	}

	@Override
	public int getNombreOrganisationsEnAttenteDansLaQueueFinale() {
		return notificationQueue.getInFinalQueueCount();
	}

	@Override
	public void demanderTraitementQueue(long noOrganisation, EvenementOrganisationProcessingMode mode) {
		notificationQueue.post(noOrganisation, mode);
	}


	/**
	 * Contrôle si un événement RCEnt a déjà été receptionné pour le businessId.
	 *
	 * @param businessId le businessId de l'événement à contrôler
	 * @return <code>true</code> si l'événement correspondant au businessId a déjà été reçu. <code>false</code> sinon.
	 */
	@Override
	public boolean dejaRecu(String businessId) {
		return !evtOrganisationDAO.getEvenementsForBusinessId(businessId).isEmpty();
	}

	/**
	 * Sauve les événements issus d'un événement organisation.
	 *
 	 * @param events événements à persister
	 * @return la liste des événements sauvés
	 */
	@Override
    @NotNull
	public List<EvenementOrganisation> saveIncomingEvent(final List<EvenementOrganisation> events) {
		Assert.isTrue(!events.isEmpty());

		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<List<EvenementOrganisation>>() {
			@NotNull
			@Override
			public List<EvenementOrganisation> doInTransaction(TransactionStatus status) {
				List<EvenementOrganisation> saved = new ArrayList<>();

				// pour les stats
				nombreEvenementsNonIgnores.incrementAndGet();

				for (EvenementOrganisation event : events) {
					final EvenementOrganisation savedEvent = evtOrganisationDAO.save(event);
					saved.add(savedEvent);
					Audit.info(event.getNoEvenement(), String.format("L'événement organisation %d pour l'organisation %d est inséré en base de données", event.getNoEvenement(), event.getNoOrganisation()));
				}
				return saved;
			}
		});
	}

	@Override
	public List<EvenementOrganisation> handleEvents(List<EvenementOrganisation> events, EvenementOrganisationProcessingMode mode) throws EvenementOrganisationException {
		for (EvenementOrganisation event : events) {
			demanderTraitementQueue(event.getNoOrganisation(), mode);
		}
		return events;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {

			// façade de monitoring sur la queue d'attente de traitement des événements organisation
			// où la charge est définie comme le nombre d'organisations en attente de traitement
			final LoadMonitorable service = new LoadMonitorable() {
				@Override
				public int getLoad() {
					return getNombreOrganisationsEnAttenteDeTraitement();
				}
			};

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
