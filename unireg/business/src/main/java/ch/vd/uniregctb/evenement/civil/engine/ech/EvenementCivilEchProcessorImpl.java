package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatEvenementCivil;

/**
 * Classe de processing des événements civils reçus de RCPers (événements e-CH)
 */
public class EvenementCivilEchProcessorImpl implements EvenementCivilEchProcessor, SmartLifecycle {

	private static final Logger LOGGER = Logger.getLogger(EvenementCivilEchProcessorImpl.class);

	private EvenementCivilNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchDAO evtCivilDAO;
	private EvenementCivilEchTranslator translator;

	private GlobalTiersIndexer indexer;
	private TiersService tiersService;

	private Processor processor;
	private final Map<Long, Listener> listeners = new LinkedHashMap<Long, Listener>();      // pour les tests, c'est pratique de conserver l'ordre (pour le reste, cela ne fait pas de mal...)

	private static final EvenementCivilEchErreurFactory ERREUR_FACTORY = new EvenementCivilEchErreurFactory();

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
	public void setIndexer(GlobalTiersIndexer indexer) {
		this.indexer = indexer;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
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
					final EvenementCivilNotificationQueue.Batch evts = notificationQueue.poll(1, TimeUnit.SECONDS);
					if (evts != null) {
						try {
							if (evts.contenu.size() > 0) {
								processEvents(evts.contenu);
							}
						}
						finally {
							notifyTraitementIndividu(evts.noIndividu);
						}
					}
				}
			}
			catch (InterruptedException e) {
				LOGGER.warn("Interruption du thread", e);
			}
			finally {
				notifyStop();
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
						if (!processEventAndDoPostProcessingOnError(evt, evts, pointer)) {
							break;
						}
						++ pointer;
					}
				}
			}
			catch (Exception e) {
				LOGGER.error(String.format("Erreur lors du traitement de l'événements civil %d", evts.get(pointer).idEvenement), e);
			}
		}

		public void requestStop() {
			stopping = true;
			LOGGER.info(String.format("Demande d'arrêt du thread %s", getName()));
		}
	}

	/**
	 * Appelé par le thread de traitement à chaque fois que les événements civils d'un individu ont été traités
	 * @param noIndividu numéro de l'individu dont les événements viennent d'être traités
	 */
	private void notifyTraitementIndividu(long noIndividu) {
		synchronized (listeners) {
			if (listeners.size() > 0) {
				for (Listener listener : listeners.values()) {
					try {
						listener.onIndividuTraite(noIndividu);
					}
					catch (Exception e) {
						// pas grave...
					}
				}
			}
		}
	}

	/**
	 * Appelé par le thread de traitement juste avant de s'arrêter
	 */
	private void notifyStop() {
		synchronized (listeners) {
			if (listeners.size() > 0) {
				for (Listener listener : listeners.values()) {
					try {
						listener.onStop();
					}
					catch (Exception e) {
						// pas grave...
					}
				}
			}
		}
	}
	
	private static final class Sequencer {
		final AtomicLong sequenceNumber = new AtomicLong(0L);
		public long next() {
			return sequenceNumber.getAndIncrement();
		}
	}

	private static final Sequencer SEQUENCER = new Sequencer();

	/**
	 * Classe interne des handles utilisés lors de l'enregistrement de listeners
	 */
	private static final class ListenerHandleImpl implements ListenerHandle {
		private final long id;
		private ListenerHandleImpl(long id) {
			this.id = id;
		}
	}

	@Override
	public ListenerHandle registerListener(Listener listener) {
		if (listener == null) {
			throw new NullPointerException("listener");
		}
		final long id = SEQUENCER.next();
		synchronized (listeners) {
			listeners.put(id, listener);
		}
		return new ListenerHandleImpl(id);
	}

	@Override
	public void unregisterListener(ListenerHandle handle) {
		if (!(handle instanceof ListenerHandleImpl)) {
			throw new IllegalArgumentException("Invalid handle");
		}
		synchronized (listeners) {
			listeners.remove(((ListenerHandleImpl) handle).id);
		}
	}

	/**
	 * Traitement de l'événement donné
	 *
	 * @param evt descripteur de l'événement civil cible à traiter
	 * @param evts liste des descripteurs d'événements (en cas d'échec sur le traitement de l'événement cible, ceux qui sont après lui dans cette liste seront passés en attente, voir {@link #errorPostProcessing(java.util.List)})
	 * @param pointer indicateur de la position de l'événement civil cible dans la liste des événements
	 * @return <code>true</code> si tout s'est bien passé, <code>false</code> si l'un au moins des événements a terminé en erreur
	 */
	protected boolean processEventAndDoPostProcessingOnError(EvenementCivilNotificationQueue.EvtCivilInfo evt, List<EvenementCivilNotificationQueue.EvtCivilInfo> evts, int pointer) {
		AuthenticationHelper.pushPrincipal(String.format("EvtCivil-%d", evt.idEvenement));
		try {
			final boolean success = processEvent(evt);
			if (!success) {
				errorPostProcessing(evts.subList(pointer + 1, evts.size()));
			}
			return success;
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	/**
	 * Classe d'exception utilisée pour wrapper une {@link EvenementCivilException}
	 */
	private static final class EvenementCivilWrappingException extends RuntimeException {
		private EvenementCivilWrappingException(EvenementCivilException cause) {
			super(cause);
		}

		@Override
		public EvenementCivilException getCause() {
			return (EvenementCivilException) super.getCause();
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
					
					try {
						return processEvent(evt);
					}
					catch (EvenementCivilException e) {
						throw new EvenementCivilWrappingException(e);
					}
				}
			});
		}
		catch (EvenementCivilWrappingException e) {
			LOGGER.error(String.format("Exception reçue lors du traitement de l'événement %d", info.idEvenement), e.getCause());
			onException(info, e.getCause());
			return false;
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
				evt.setDateTraitement(DateHelper.getCurrentDate());
				Audit.error(info.idEvenement, "Statut de l'événement passé à 'EN_ERREUR'");
				return null;
			}
		});
	}

	/**
	 * Quand la méthode {@link #processEventAndDoPostProcessingOnError} a renvoyé <code>false</code>, il faut passer tous les événements
	 * restant de la liste de l'état "A_TRAITER" en "EN_ATTENTE"
	 * @param remainingEvents descriptif des événements dans la queue
	 */
	private void errorPostProcessing(final List<EvenementCivilNotificationQueue.EvtCivilInfo> remainingEvents) {
		if (remainingEvents != null && remainingEvents.size() > 0) {
			final List<EvenementCivilNotificationQueue.EvtCivilInfo> pourIndexation = doInNewTransaction(new TransactionCallback<List<EvenementCivilNotificationQueue.EvtCivilInfo>>() {
				@Override
				public List<EvenementCivilNotificationQueue.EvtCivilInfo> doInTransaction(TransactionStatus status) {
					final List<EvenementCivilNotificationQueue.EvtCivilInfo> pourIndexation = new ArrayList<EvenementCivilNotificationQueue.EvtCivilInfo>(remainingEvents.size()); 
					for (EvenementCivilNotificationQueue.EvtCivilInfo info : remainingEvents) {
						if (info.etat == EtatEvenementCivil.A_TRAITER) {
							final EvenementCivilEch evt = evtCivilDAO.get(info.idEvenement);
							if (evt.getEtat() == EtatEvenementCivil.A_TRAITER) {        // re-test pour vérifier que l'information dans le descripteur est toujours à jour
								if (translator.isIndexationOnly(evt)) {
									pourIndexation.add(info);
								}
								else {
									evt.setEtat(EtatEvenementCivil.EN_ATTENTE);
									Audit.info(evt.getId(), String.format("Mise en attente de l'événement %d", evt.getId()));
								}
							}
						}
					}
					return pourIndexation;
				}
			});
			
			// on a mis tout le monde en attente, maintenant on va essayer de traiter les cas pour lesquels en gros seule une réindexation est nécessaire
			if (pourIndexation.size() > 0) {
				int pointer = 0;
				try {
					LOGGER.info("Lancement du traitement des événements d'indexation pure restants");
					for (EvenementCivilNotificationQueue.EvtCivilInfo info : pourIndexation) {
						if (!processEventAndDoPostProcessingOnError(info, pourIndexation, pointer)) {
							// si on reviens avec <code>false</code>, c'est qu'on a essayé de re-traiter les suivants aussi
							break;
						}
						++ pointer;
					}
				}
				catch (Exception e) {
					LOGGER.error(String.format("Erreur lors du traitement de l'événements civil %d", pourIndexation.get(pointer).idEvenement), e);
				}
			}
		}
	}

	/**
	 * Demande une ré-indexation du tiers lié à l'individu dont l'identifiant est fourni (doit être appelé dans un
	 * context transactionnel)
	 * @param noIndividu identifiant d'individu
	 */
	private void scheduleIndexation(long noIndividu) {
		final PersonnePhysique pp = tiersService.getPersonnePhysiqueByNumeroIndividu(noIndividu);
		if (pp != null) {
			indexer.schedule(pp.getNumero());
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
	 * @throws ch.vd.uniregctb.evenement.civil.common.EvenementCivilException en cas de problème métier
	 */
	private boolean processEvent(EvenementCivilEch event) throws EvenementCivilException {
		Audit.info(event.getId(), String.format("Début du traitement de l'événement civil %d de type %s/%s au %s sur l'individu %d", event.getId(), event.getType(), event.getAction(), RegDateHelper.dateToDisplayString(event.getDateEvenement()), event.getNumeroIndividu()));

		// élimination des erreurs en cas de retraitement
		event.getErreurs().clear();

		final EvenementCivilMessageCollector<EvenementCivilEchErreur> collector = new EvenementCivilMessageCollector<EvenementCivilEchErreur>(ERREUR_FACTORY);
		final EtatEvenementCivil etat = processEventAndCollectMessages(event, collector, collector);

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
			Audit.success(event.getId(), String.format("Statut de l'événement passé à '%s'", etat.name()));

			// dans les cas "redondants", on n'a touché à rien, mais il est peut-être utile de forcer une ré-indexation quand-même, non ?
			if (etat == EtatEvenementCivil.REDONDANT) {
				scheduleIndexation(event.getNumeroIndividu());
			}
		}

		return !hasErrors;
	}

	private EtatEvenementCivil processEventAndCollectMessages(EvenementCivilEch event, EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
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
