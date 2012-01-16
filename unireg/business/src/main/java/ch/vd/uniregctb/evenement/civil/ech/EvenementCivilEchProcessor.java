package ch.vd.uniregctb.evenement.civil.ech;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.context.SmartLifecycle;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.civil.engine.EvenementCivilNotificationQueue;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * Classe de processing des événements civils reçus de RCPers (événements e-CH)
 */
public class EvenementCivilEchProcessor implements SmartLifecycle {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchProcessor.class);

	private EvenementCivilNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchDAO evtCivilDAO;

	private Processor processor;

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

	/**
	 * Thread de traitement
	 */
	private class Processor extends Thread {

		private boolean stopping = false;

		public Processor() {
			super("EvtCivilEch");
		}

		@Override
		public void run() {
			LOGGER.info(String.format("Démarrage du thread %s de traitement des événements civils e-CH", getName()));
			try {
				while (!stopping) {
					final List<EvenementCivilNotificationQueue.EvtCivilInfo> evts = notificationQueue.poll(1, TimeUnit.SECONDS);
					if (evts != null && evts.size() > 0) {
						processEvents(evts);
					}
				}
			}
			catch (InterruptedException e) {
				LOGGER.warn("Interruption du thread", e);
			}
			finally {
				LOGGER.info(String.format("Arrêt du thread %s", getName()));
			}
		}

		/**
		 * Prend les événements dans l'ordre et essaie de les traiter. S'arrête à la première erreur.
		 * @param evts descriptifs des événements à traiter
		 */
		private void processEvents(List<EvenementCivilNotificationQueue.EvtCivilInfo> evts) {
			int pointer = 0;
			try {
				LOGGER.info(String.format("Lancement du traitement d'un lot de %d événement(s) pour l'individu %d", evts.size(), evts.get(0).noIndividu));
				for (EvenementCivilNotificationQueue.EvtCivilInfo evt : evts) {
					if (!stopping) {
						AuthenticationHelper.pushPrincipal(String.format("EvtCivil-%d", evt.idEvenement));
						try {
							final boolean success = processEvent(evt);
							if (!success) {
								errorPostProcessing(evts.subList(pointer + 1, evts.size()));
								break;
							}
							++ pointer;
						}
						finally {
							AuthenticationHelper.popPrincipal();
						}
					}
				}
			}
			catch (Exception e) {
				LOGGER.error(String.format("Erreur lors du traitement des événements civils %d", evts.get(pointer).idEvenement), e);
			}
		}

		public void requestStop() {
			stopping = true;
			LOGGER.info(String.format("Demande d'arrêt du thread %s", getName()));
		}
	}

	/**
	 * Lancement du processing de l'événement civil décrit dans la structure donnée
	 * @param info description de l'événement civil à traiter maintenant
	 * @return <code>true</code> si tout s'est bien passé et que l'on peut continuer sur les événements suivants, <code>false</code> si on ne doit pas continuer
	 */
	private boolean processEvent(final EvenementCivilNotificationQueue.EvtCivilInfo info) {
		return doInNewTransaction(new TransactionCallback<Boolean>() {
			@Override
			public Boolean doInTransaction(TransactionStatus status) {
				final EvenementCivilEch evt = evtCivilDAO.get(info.idEvenement);
				if (evt.getEtat().isTraite()) {
					LOGGER.info(String.format("Evénement %d déjà dans l'état %s, on ne le re-traite pas", info.idEvenement, evt.getEtat()));
					return Boolean.TRUE;
				}
				return processEvent(evt);
			}
		});
	}

	/**
	 * Quand la méthode {@link #processEvent} a renvoyé <code>false</code>, il faut passer tous les événements
	 * restant de la liste de l'état "A_TRAITER" en "EN_ATTENTE"
	 * @param remainingEvents descriptif des événements dans la queue
	 */
	private void errorPostProcessing(final List<EvenementCivilNotificationQueue.EvtCivilInfo> remainingEvents) {
		if (remainingEvents != null && remainingEvents.size() > 0) {
			doInNewTransaction(new TransactionCallback<Object>() {
				@Override
				public Object doInTransaction(TransactionStatus status) {
					for (EvenementCivilNotificationQueue.EvtCivilInfo info : remainingEvents) {
						if (info.etat == EtatEvenementCivil.A_TRAITER) {
							final EvenementCivilEch evt = evtCivilDAO.get(info.idEvenement);
							if (evt.getEtat() == EtatEvenementCivil.A_TRAITER) {
								evt.setEtat(EtatEvenementCivil.EN_ATTENTE);
								Audit.info(evt.getId(), String.format("Mise en attente de l'événement %d", evt.getId()));
							}
						}
					}
					return null;
				}
			});
		}
	}

	private <T> T doInNewTransaction(final TransactionCallback<T> action) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(action);
	}

	/**
	 * Appelé dans une transaction pour lancer le traitement de l'événement civil
	 * @param event événement à traiter
	 * @return <code>true</code> si tout s'est bien passé, <code>false</code> si l'événement a été mis en erreur
	 */
	private boolean processEvent(EvenementCivilEch event) {
		Audit.info(event.getId(), String.format("Début du traitement de l'événement civil %d de type %s/%s au %s sur l'individu %d", event.getId(), event.getType(), event.getAction(), RegDateHelper.dateToDisplayString(event.getDateEvenement()), event.getNumeroIndividu()));

		// TODO jde à implémenter
		event.setEtat(EtatEvenementCivil.EN_ERREUR);
		event.setCommentaireTraitement("A implémenter...");
		return false;
	}

	@Override
	public boolean isAutoStartup() {
		return true;
	}

	@Override
	public void stop(Runnable callback) {
		stop();
		callback.run();
	}

	@Override
	public void start() {
		if (processor == null) {
			processor = new Processor();
			processor.start();
		}
	}

	@Override
	public void stop() {
		if (processor != null) {
			processor.requestStop();
			try {
				processor.join();
			}
			catch (InterruptedException e) {
				// au moins, on aura essayé...
				LOGGER.warn("Attente de terminaison du thread de traitement des événements civils e-CH interrompue", e);
			}
			processor = null;
		}
	}

	@Override
	public boolean isRunning() {
		return processor != null && processor.isAlive();
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;   // as late as possible during starting process
	}
}
