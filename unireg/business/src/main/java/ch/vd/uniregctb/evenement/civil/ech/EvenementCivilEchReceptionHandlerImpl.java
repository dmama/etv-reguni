package ch.vd.uniregctb.evenement.civil.ech;

import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.evd0001.v3.Person;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilNotificationQueue;
import ch.vd.uniregctb.interfaces.model.impl.IndividuRCPers;

public class EvenementCivilEchReceptionHandlerImpl implements EvenementCivilEchReceptionHandler {

	private RcPersClient rcPersClient;
	private EvenementCivilNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchDAO evtCivilDAO;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setRcPersClient(RcPersClient rcPersClient) {
		this.rcPersClient = rcPersClient;
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
		sendNotificationForNewEvent(noIndividu);
		return event;
	}

	private long getNumeroIndividuPourEvent(EvenementCivilEch event) throws EvenementCivilException {
		if (event.getNumeroIndividu() != null) {
			return event.getNumeroIndividu();
		}
		else {
			final Person person = rcPersClient.getPersonForEvent(event.getId());
			if (person == null) {
				throw new EvenementCivilException(String.format("Pas d'individu lié à l'événement civil %d", event.getId()));
			}
			return IndividuRCPers.getNoIndividu(person);
		}
	}

	private EvenementCivilEch assignNumeroIndividu(final EvenementCivilEch event, final long numeroIndividu) {
		if (event.getNumeroIndividu() == null || event.getNumeroIndividu() != numeroIndividu) {
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			return template.execute(new TransactionCallback<EvenementCivilEch>() {
				@Override
				public EvenementCivilEch doInTransaction(TransactionStatus status) {
					event.setNumeroIndividu(numeroIndividu);
					return evtCivilDAO.save(event);
				}
			});
		}
		else {
			return event;
		}
	}

	private void sendNotificationForNewEvent(long noIndividu) {
		notificationQueue.add(noIndividu);
	}
}
