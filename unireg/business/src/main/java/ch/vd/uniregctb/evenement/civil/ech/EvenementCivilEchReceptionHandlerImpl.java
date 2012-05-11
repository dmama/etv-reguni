package ch.vd.uniregctb.evenement.civil.ech;

import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilNotificationQueue;
import org.jetbrains.annotations.Nullable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.atomic.AtomicInteger;

public class EvenementCivilEchReceptionHandlerImpl implements EvenementCivilEchReceptionHandler, EvenementCivilEchReceptionMonitor {

	private EvenementCivilNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchDAO evtCivilDAO;
    private EvenementCivilEchService evtCivilService;
	
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

    @Override
	public int getNombreEvenementsNonIgnores() {
		return nombreEvenementsNonIgnores.intValue();
	}

	@Override
	public int getNombreIndividusEnAttenteDeTraitement() {
		return notificationQueue.getInflightCount();
	}

	@Override
	public void demanderTraitementQueue(long noIndividu, boolean immediate) {
		notificationQueue.post(noIndividu, immediate);
	}

	@Override
    @Nullable
	public EvenementCivilEch saveIncomingEvent(final EvenementCivilEch event) {
		final long id = event.getId();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<EvenementCivilEch>() {
			@Nullable
            @Override
			public EvenementCivilEch doInTransaction(TransactionStatus status) {

				// si un événement civil existe déjà avec l'ID donné, on log un warning et on s'arrête là...
				if (evtCivilDAO.exists(id)) {
					Audit.warn(id, String.format("L'événement civil %d existe déjà en base : cette nouvelle réception est donc ignorée!", id));
					return null;
				}

				// pour les stats
				nombreEvenementsNonIgnores.incrementAndGet();

				final EvenementCivilEch saved = evtCivilDAO.save(event);
				Audit.info(id, String.format("L'événement civil %d est inséré en base de données", id));
				return saved;
			}
		});
	}

	@Override
	public EvenementCivilEch handleEvent(EvenementCivilEch event) throws EvenementCivilException {
		// récupération de l'individu
		final long noIndividu = evtCivilService.getNumeroIndividuPourEvent(event);

		// sauvegarde de l'individu dans l'événement
        event = evtCivilService.assigneNumeroIndividu(event, noIndividu);

		// notification du moteur de traitement
		demanderTraitementQueue(noIndividu, false);
		return event;
	}



}
