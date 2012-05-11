package ch.vd.uniregctb.evenement.civil.ech;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.unireg.interfaces.civil.data.Individu;
import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.engine.ech.EvenementCivilNotificationQueue;
import ch.vd.uniregctb.interfaces.service.rcpers.RcPersClientHelper;

public class EvenementCivilEchReceptionHandlerImpl implements EvenementCivilEchReceptionHandler, EvenementCivilEchReceptionMonitor {

	private RcPersClientHelper rcPersClientHelper;
	private EvenementCivilNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchDAO evtCivilDAO;
	
	private final AtomicInteger nombreEvenementsNonIgnores = new AtomicInteger(0);

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRcPersClientHelper(RcPersClientHelper rcPersClientHelper) {
		this.rcPersClientHelper = rcPersClientHelper;
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
	public EvenementCivilEch saveIncomingEvent(final EvenementCivilEch event) {
		final long id = event.getId();
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(new TransactionCallback<EvenementCivilEch>() {
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
		// 4. récupération de l'individu
		final long noIndividu = getNumeroIndividuPourEvent(event);

		// 5. sauvegarde de l'individu dans l'événement
		event = assignNumeroIndividu(event, noIndividu);

		// 6. notification du moteur de traitement
		demanderTraitementQueue(noIndividu, false);
		return event;
	}

	private long getNumeroIndividuPourEvent(EvenementCivilEch event) throws EvenementCivilException {
		if (event.getNumeroIndividu() != null) {
			return event.getNumeroIndividu();
		}
		else {
			final IndividuApresEvenement apresEvenement = rcPersClientHelper.getIndividuFromEvent(event.getId());
			if (apresEvenement == null) {
				throw new EvenementCivilException(String.format("Pas d'événement RcPers lié à l'événement civil %d", event.getId()));
			}
			final Individu individu = apresEvenement.getIndividu();
			if (individu == null) {
				throw new EvenementCivilException(String.format("Aucune donnée d'individu fournie avec l'événement civil %d", event.getId()));
			}
			return individu.getNoTechnique();
		}
	}

	private EvenementCivilEch assignNumeroIndividu(final EvenementCivilEch event, final long numeroIndividu) {
		if (event.getNumeroIndividu() == null || event.getNumeroIndividu() != numeroIndividu) {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			return template.execute(new TransactionCallback<EvenementCivilEch>() {
				@Override
				public EvenementCivilEch doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = evtCivilDAO.get(event.getId());
					evt.setNumeroIndividu(numeroIndividu);
					return evtCivilDAO.save(evt);
				}
			});
		}
		else {
			return event;
		}
	}
}
