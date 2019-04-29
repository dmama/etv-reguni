package ch.vd.unireg.evenement.civil.ech;

import java.util.concurrent.atomic.AtomicInteger;

import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.engine.ech.EvenementCivilNotificationQueue;
import ch.vd.unireg.load.BasicLoadMonitor;
import ch.vd.unireg.load.LoadAverager;
import ch.vd.unireg.stats.LoadMonitorable;
import ch.vd.unireg.stats.StatsService;

public class EvenementCivilEchReceptionHandlerImpl implements EvenementCivilEchReceptionHandler, EvenementCivilEchReceptionMonitor, InitializingBean, DisposableBean {

	private static final String SERVICE_NAME = "EvtsCivilsEchQueueSize";

	private EvenementCivilNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchDAO evtCivilDAO;
	private EvenementCivilEchService evtCivilService;
	private StatsService statsService;
	private LoadAverager loadAverager;
	private AuditManager audit;

	private final AtomicInteger nombreEvenementsNonIgnores = new AtomicInteger(0);

    public EvenementCivilEchReceptionHandlerImpl() {
    }

    @SuppressWarnings({"UnusedDeclaration"})
	public void setNotificationQueue(EvenementCivilNotificationQueue notificationQueue) {
		this.notificationQueue = notificationQueue;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtCivilDAO(EvenementCivilEchDAO evtCivilDAO) {
		this.evtCivilDAO = evtCivilDAO;
	}

    @SuppressWarnings({"UnusedDeclaration"})
    public void setEvtCivilService(EvenementCivilEchService evtCivilService) {
        this.evtCivilService = evtCivilService;
    }

	@SuppressWarnings({"UnusedDeclaration"})
	public void setStatsService(StatsService statsService) {
		this.statsService = statsService;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}

	@Override
	public int getNombreEvenementsNonIgnores() {
		return nombreEvenementsNonIgnores.intValue();
	}

	@Override
	public int getNombreIndividusEnAttenteDeTraitement() {
		return notificationQueue.getTotalCount();
	}

	@Override
	public int getNombreIndividusEnAttenteDansLaQueueBatch() {
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
	public int getNombreIndividusEnAttenteDansLaQueueManuelle() {
		return notificationQueue.getInManualQueueCount();
	}

	@Override
	public Long getMoyenneGlissanteDureeAttenteDansLaQueueManuelle() {
		return notificationQueue.getManualQueueSlidingAverageAge();
	}

	@Override
	public Long getMoyenneTotaleDureeAttenteDansLaQueueManuelle() {
		return notificationQueue.getManualQueueGlobalAverageAge();
	}

	@Override
	public int getNombreIndividusEnAttenteDansLaQueueImmediate() {
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
	public int getNombreIndividusEnTransitionVersLaQueueFinale() {
		return notificationQueue.getInHatchesCount();
	}

	@Override
	public int getNombreIndividusEnAttenteDansLaQueueFinale() {
		return notificationQueue.getInFinalQueueCount();
	}

	@Override
	public void demanderTraitementQueue(long noIndividu, EvenementCivilEchProcessingMode mode) {
		notificationQueue.post(noIndividu, mode);
	}

	@Override
    @Nullable
	public EvenementCivilEch saveIncomingEvent(final EvenementCivilEch event) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(status -> {
			// si un événement civil existe déjà avec l'ID donné, on log un warning et on s'arrête là...
			final long id = event.getId();
			if (evtCivilDAO.exists(id)) {
				audit.warn(id, String.format("L'événement civil %d existe déjà en base : cette nouvelle réception est donc ignorée!", id));
				return null;
			}

			// pour les stats
			nombreEvenementsNonIgnores.incrementAndGet();

			final EvenementCivilEch saved = evtCivilDAO.save(event);
			audit.info(id, String.format("L'événement civil %d est inséré en base de données", id));
			return saved;
		});
	}

	@Override
	public EvenementCivilEch handleEvent(EvenementCivilEch event, @Nullable EvenementCivilEchProcessingMode mode) throws EvenementCivilException {
		// récupération de l'individu
		final long noIndividu = evtCivilService.getNumeroIndividuPourEvent(event);

		// sauvegarde de l'individu dans l'événement
		if (event.getNumeroIndividu() == null || event.getNumeroIndividu() != noIndividu) {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			final EvenementCivilEch oldEvent = event;
			event = template.execute(status -> evtCivilService.assigneNumeroIndividu(oldEvent, noIndividu));
		}

		// notification du moteur de traitement
		if (mode != null) {
			demanderTraitementQueue(noIndividu, mode);
		}
		return event;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		if (statsService != null) {

			// façade de monitoring sur la queue d'attente de traitement des événements civils
			// où la charge est définie comme le nombre d'individus en attente de traitement
			final LoadMonitorable service = this::getNombreIndividusEnAttenteDeTraitement;

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
