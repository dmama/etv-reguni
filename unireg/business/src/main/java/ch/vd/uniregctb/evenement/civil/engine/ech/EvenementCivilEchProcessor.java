package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.context.SmartLifecycle;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilHelper;
import ch.vd.uniregctb.evenement.civil.EvenementCivilMessageCollector;
import ch.vd.uniregctb.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilException;
import ch.vd.uniregctb.evenement.civil.common.EvenementCivilOptions;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEch;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.uniregctb.evenement.civil.ech.EvenementCivilEchErreurFactory;
import ch.vd.uniregctb.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * Classe de processing des événements civils reçus de RCPers (événements e-CH)
 */
public class EvenementCivilEchProcessor implements SmartLifecycle {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchProcessor.class);

	private EvenementCivilNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchDAO evtCivilDAO;
	private EvenementCivilEchTranslator translator;

	private Processor processor;
	private ProcessingMonitor monitor;

	private static final EvenementCivilEchErreurFactory ERREUR_FACTORY = new EvenementCivilEchErreurFactory();

	/**
	 * Interface utilisable dans les tests afin de réagir au traitement d'un événement civil
	 */
	public static interface ProcessingMonitor {
		/**
		 * Appelé à la fin du traitement de l'événement identifié
		 * @param evtId identifiant de l'événement civil pour lequel le traitement vient de se terminer
		 */
		void onProcessingEnd(long evtId);
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
	public void setTranslator(EvenementCivilEchTranslator translator) {
		this.translator = translator;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setMonitor(ProcessingMonitor monitor) {
		this.monitor = monitor;
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
							if (monitor != null) {
								monitor.onProcessingEnd(evt.idEvenement);
							}
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
		try {
			return doInNewTransaction(new TransactionCallback<Boolean>() {
				@Override
				public Boolean doInTransaction(TransactionStatus status) {
					final EvenementCivilEch evt = evtCivilDAO.get(info.idEvenement);
					if (evt == null) {
						LOGGER.warn(String.format("Pas d'événement trouvé correspondant à l'identifiant %d", info.idEvenement));
						return Boolean.TRUE;
					}
					else if (evt.getEtat().isTraite()) {
						LOGGER.info(String.format("Evénement %d déjà dans l'état %s, on ne le re-traite pas", info.idEvenement, evt.getEtat()));
						return Boolean.TRUE;
					}
					return processEvent(evt);
				}
			});
		}
		catch (Exception e) {
			LOGGER.error(String.format("Exception reçue lors du traitement de l'événement %d", info.idEvenement), e);
			onException(info, e);
			return false;
		}
	}

	/**
	 * Assigne le message d'erreur à l'événement en fonction de l'exception
	 * @param info description de l'événement en cours de traitement
	 * @param e exception qui a sauté
	 */
	private void onException(final EvenementCivilNotificationQueue.EvtCivilInfo info, final Exception e) {
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEchErreur erreur = ERREUR_FACTORY.createErreur(e);
				final EvenementCivilEch evt = evtCivilDAO.get(info.idEvenement);
				evt.getErreurs().clear();
				evt.getErreurs().add(erreur);
				evt.setEtat(EtatEvenementCivil.EN_ERREUR);
				return null;
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

		// élimination des erreurs en cas de retraitement
		event.getErreurs().clear();

		final EvenementCivilMessageCollector<EvenementCivilEchErreur> collector = new EvenementCivilMessageCollector<EvenementCivilEchErreur>(ERREUR_FACTORY);
		final EtatEvenementCivil etat = processEvent(event, collector, collector);

		// les erreurs et warnings collectés sont maintenant associés à l'événement en base
		final List<EvenementCivilEchErreur> erreurs = EvenementCivilHelper.eliminerDoublons(collector.getErreurs());
		final List<EvenementCivilEchErreur> warnings = EvenementCivilHelper.eliminerDoublons(collector.getWarnings());
		event.getErreurs().addAll(erreurs);
		event.getErreurs().addAll(warnings);
		event.setDateTraitement(DateHelper.getCurrentDate());

		for (EvenementCivilEchErreur e : erreurs) {
			Audit.error(event.getId(), e.getMessage());
		}
		for (EvenementCivilEchErreur w : warnings) {
			Audit.warn(event.getId(), w.getMessage());
		}

		final boolean hasErrors = collector.hasErreurs();
		if (hasErrors || etat == EtatEvenementCivil.EN_ERREUR) {
			event.setEtat(EtatEvenementCivil.EN_ERREUR);
			Audit.error(event.getId(), "Statut de l'événement passé à 'EN_ERREUR'");
		}
		else if (collector.hasWarnings() || etat == EtatEvenementCivil.A_VERIFIER) {
			event.setEtat(EtatEvenementCivil.A_VERIFIER);
			Audit.warn(event.getId(), "Statut de l'événement passé à 'A_VERIFIER'");
		}
		else {
			event.setEtat(etat);
			Audit.success(event.getId(), "Statut de l'événement passé à '" + etat.name() + "'");
		}

		return !hasErrors;
	}

	private EtatEvenementCivil processEvent(EvenementCivilEch event, EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) {
		try {
			final EvenementCivilInterne evtInterne = buildInterne(event);
			if (evtInterne == null) {
				LOGGER.error(String.format("Aucun code de traitement trouvé pour l'événement %d", event.getId()));
				erreurs.addErreur("Aucun code de traitement trouvé");
				return EtatEvenementCivil.EN_ERREUR;
			}
			else {
				// validation et traitement
				evtInterne.validate(erreurs, warnings);
				if (erreurs.hasErreurs()) {
					return EtatEvenementCivil.EN_ERREUR;
				}

				return evtInterne.handle(warnings).toEtat();
			}
		}
		catch (EvenementCivilException e) {
			LOGGER.error(String.format("Exception lancée lors du traitement de l'événement %d", event.getId()), e);
			erreurs.addErreur(e);
			return EtatEvenementCivil.EN_ERREUR;
		}
	}

	private EvenementCivilInterne buildInterne(EvenementCivilEch event) throws EvenementCivilException {
		final EvenementCivilOptions options = new EvenementCivilOptions(true);
		return translator.toInterne(event, options);
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
