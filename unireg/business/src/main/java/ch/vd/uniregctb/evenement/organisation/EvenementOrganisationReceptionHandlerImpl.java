package ch.vd.uniregctb.evenement.organisation;

import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.organisation.engine.EvenementOrganisationNotificationQueue;
import ch.vd.uniregctb.load.BasicLoadMonitor;
import ch.vd.uniregctb.load.LoadAverager;
import ch.vd.uniregctb.load.LoadMonitorable;
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
		return notificationQueue.getInBatchQueueCount();
	}

	@Override
	public Long getMoyenneGlissanteDureeAttenteDansLaQueueBatch() {
		return notificationQueue.getBatchQueueSlidingAverageAge();
	}

	@Override
	public Long getMoyenneTotaleDureeAttenteDansLaQueueBatch() {
		return notificationQueue.getBatchQueueGlobalAverageAge();
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


	@Override
    @Nullable
	public EvenementOrganisation saveIncomingEvent(final EvenementOrganisation event) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<EvenementOrganisation>() {
			@Nullable
            @Override
			public EvenementOrganisation doInTransaction(TransactionStatus status) {

				// si un événement organnisation existe déjà avec l'ID donné, on log un warning et on s'arrête là...
				final long id = event.getId();
				if (evtOrganisationDAO.exists(id)) {
					Audit.warn(id, String.format("L'événement organnisation %d existe déjà en base : cette nouvelle réception est donc ignorée!", id));
					return null;
				}

				// pour les stats
				nombreEvenementsNonIgnores.incrementAndGet();

				final EvenementOrganisation saved = evtOrganisationDAO.save(event);
				Audit.info(id, String.format("L'événement organnisation %d est inséré en base de données", id));
				return saved;
			}
		});
	}

	@Override
	public EvenementOrganisation handleEvent(EvenementOrganisation event, EvenementOrganisationProcessingMode mode) throws EvenementOrganisationException {
		demanderTraitementQueue(event.getNoOrganisation(), mode);
		return event;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {

			// façade de monitoring sur la queue d'attente de traitement des événements organnisation
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
