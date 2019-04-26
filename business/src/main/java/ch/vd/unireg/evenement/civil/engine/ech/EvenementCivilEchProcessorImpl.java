package ch.vd.unireg.evenement.civil.engine.ech;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.hibernate.CallbackException;
import org.hibernate.type.Type;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.HibernateEntity;
import ch.vd.unireg.common.LengthConstants;
import ch.vd.unireg.common.PollingThread;
import ch.vd.unireg.data.DataEventService;
import ch.vd.unireg.evenement.EvenementCivilHelper;
import ch.vd.unireg.evenement.civil.EvenementCivilErreurCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilMessageCollector;
import ch.vd.unireg.evenement.civil.EvenementCivilWarningCollector;
import ch.vd.unireg.evenement.civil.common.EvenementCivilException;
import ch.vd.unireg.evenement.civil.common.EvenementCivilOptions;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEch;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchBasicInfo;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchDAO;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreur;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchErreurFactory;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchFacade;
import ch.vd.unireg.evenement.civil.ech.EvenementCivilEchWrappingFacade;
import ch.vd.unireg.evenement.civil.interne.EvenementCivilInterne;
import ch.vd.unireg.hibernate.interceptor.ModificationInterceptor;
import ch.vd.unireg.hibernate.interceptor.ModificationSubInterceptor;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.interfaces.service.ServiceCivilService;
import ch.vd.unireg.parentes.ParentesSynchronizerInterceptor;
import ch.vd.unireg.tiers.PersonnePhysique;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.ActionEvenementCivilEch;
import ch.vd.unireg.type.EtatEvenementCivil;

/**
 * Classe de processing des événements civils reçus de RCPers (événements e-CH)
 */
public class EvenementCivilEchProcessorImpl implements EvenementCivilEchProcessor, EvenementCivilEchInternalProcessor, SmartLifecycle, InitializingBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementCivilEchProcessorImpl.class);

	private static final String COMMENTAIRE_ANNULATION_GROUPEE = "Groupe d'événements annulés alors qu'ils étaient encore en attente.";
	private static final String COMMENTAIRE_CORRECTION_GROUPEE = "Evénement directement pris en compte dans le traitement de l'événement référencé.";
	private static final String COMMENTAIRE_ACTION_CORRECTIVE_GROUPEE = "Evénement et correction(s) pris en compte ensemble.";

	private EvenementCivilNotificationQueue notificationQueue;
	private PlatformTransactionManager transactionManager;
	private EvenementCivilEchDAO evtCivilDAO;
	private EvenementCivilEchTranslator translator;
	private DataEventService dataEventService;

	private GlobalTiersIndexer indexer;
	private TiersService tiersService;
	private ServiceCivilService serviceCivil;

	private List<ErrorPostProcessingStrategy> postProcessingStrategies;

	private ModificationInterceptor mainInterceptor;
	private ParentesSynchronizerInterceptor parentesSynchronizerInterceptor;
	private AuditManager audit;

	private Processor processor;
	private final Map<Long, Listener> listeners = new LinkedHashMap<>();      // pour les tests, c'est pratique de conserver l'ordre (pour le reste, cela ne fait pas de mal...)

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

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	public void setMainInterceptor(ModificationInterceptor mainInterceptor) {
		this.mainInterceptor = mainInterceptor;
	}

	public void setParentesSynchronizerInterceptor(ParentesSynchronizerInterceptor parentesSynchronizerInterceptor) {
		this.parentesSynchronizerInterceptor = parentesSynchronizerInterceptor;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}

	/**
	 * Thread de traitement
	 */
	private class Processor extends PollingThread<EvenementCivilNotificationQueue.Batch> {

		private Processor() {
			super("EvtCivilEch");
		}

		@Override
		protected EvenementCivilNotificationQueue.Batch poll(@NotNull Duration pollingTimeout) throws InterruptedException {
			return notificationQueue.poll(pollingTimeout);
		}

		@Override
		protected void processElement(@NotNull EvenementCivilNotificationQueue.Batch element) {
			if (element.contenu.size() > 0) {
				processEvents(element.noIndividu, element.contenu);
			}
		}

		@Override
		protected void onElementProcessed(@NotNull EvenementCivilNotificationQueue.Batch element, @Nullable Throwable t) {
			super.onElementProcessed(element, t);
			notifyTraitementIndividu(element.noIndividu);
		}

		@Override
		protected void onStop() {
			super.onStop();
			notifyStop();
		}

		/**
		 * Prend les événements dans l'ordre et essaie de les traiter. S'arrête à la première erreur.
		 * @param noIndividu identifiant de l'individu pour lequel des événements doivent être traités
		 * @param evts descriptifs des événements à traiter
		 */
		private void processEvents(long noIndividu, List<EvenementCivilEchBasicInfo> evts) {
			int pointer = 0;
			final long start = System.nanoTime();
			try {
				LOGGER.info(String.format("Lancement du traitement d'un lot de %d événement(s) pour l'individu %d", evts.size(), noIndividu));
				for (EvenementCivilEchBasicInfo evt : evts) {
					if (!shouldStop()) {
						if (!processEventAndDoPostProcessingOnError(evt, evts, pointer)) {
							break;
						}
						++ pointer;
					}
				}
			}
			catch (Exception e) {
				LOGGER.error(String.format("Erreur lors du traitement de l'événement civil %d", evts.get(pointer).getId()), e);
			}
			finally {
				final long end = System.nanoTime();
				LOGGER.info(String.format("Lot de %d événement(s) traité en %d ms", evts.size(), TimeUnit.NANOSECONDS.toMillis(end - start)));
			}
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
		private final AtomicLong sequenceNumber = new AtomicLong(0L);
		public long next() {
			return sequenceNumber.getAndIncrement();
		}
	}

	private static final Sequencer SEQUENCER = new Sequencer();

	/**
	 * Classe interne des handles utilisés lors de l'enregistrement de listeners
	 */
	private final class ListenerHandleImpl implements ListenerHandle {
		private final long id;
		private ListenerHandleImpl(long id) {
			this.id = id;
		}

		@Override
		public void unregister() {
			synchronized (listeners) {
				if (listeners.remove(id) == null) {
					throw new IllegalStateException("Already unregistered!");
				}
			}
		}
	}

	@NotNull
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

	/**
	 * Traitement de l'événement donné
	 *
	 * @param evt descripteur de l'événement civil cible à traiter
	 * @param evts liste des descripteurs d'événements (en cas d'échec sur le traitement de l'événement cible, ceux qui sont après lui dans cette liste seront passés en attente, voir {@link #errorPostProcessing(java.util.List)})
	 * @param pointer indicateur de la position de l'événement civil cible dans la liste des événements
	 * @return <code>true</code> si tout s'est bien passé, <code>false</code> si l'un au moins des événements a terminé en erreur
	 */
	public boolean processEventAndDoPostProcessingOnError(EvenementCivilEchBasicInfo evt, List<EvenementCivilEchBasicInfo> evts, int pointer) {
		AuthenticationHelper.pushPrincipal(String.format("EvtCivil-%d", evt.getId()));
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
	 * Intercepteur qui suit la transaction en cours pour forcer, de manière systématique, le rafraîchissement
	 * des données de parentés sur la personne physique correspondant à l'événement reçu.<p/>
	 * On utilise ici cette mécanique d'intercepteur pour deux raisons principales :
	 * <ul>
	 *     <li>systématique du comportement&nbsp;;</li>
	 *     <li>pour s'assurer que, dans le cas où la personne physique est modifiée par le traitement de l'événement, le rafraîchissement ne soit pas fait deux fois.</li>
	 * </ul>
	 */
	private final class ParenteRefreshForcingInterceptor implements ModificationSubInterceptor {

		private final long noIndividu;

		private ParenteRefreshForcingInterceptor(long noIndividu) {
			this.noIndividu = noIndividu;
		}

		@Override
		public boolean onChange(HibernateEntity entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types, boolean isAnnulation) throws CallbackException {
			// rien à faire ici
			return false;
		}

		@Override
		public void postFlush() throws CallbackException {
			// ceci doit être fait avant les appels à {@link #postTransactionCommit()}... donc ici c'est bon !
			// (faire cet appel dans le {@link #preTransactionCommit()} est en fait même déjà trop tard, sans doute parce que le flush JTA
			// est maintenant lancé depuis un preTransactionCommit() - voir UniregJtaTransactionManager)
			// TODO peut-être que la vraie solution passe par un ordonnancement des SubInterceptors...
			parentesSynchronizerInterceptor.forceRefreshOnIndividu(noIndividu);
		}

		@Override
		public void suspendTransaction() {
			// rien à faire ici
		}

		@Override
		public void resumeTransaction() {
			// rien à faire ici
		}

		@Override
		public void preTransactionCommit() {
			// rien à faire ici
		}

		@Override
		public void postTransactionCommit() {
			// rien à faire ici
		}

		@Override
		public void postTransactionRollback() {
			// rien à faire ici
		}
	}

	/**
	 * Lancement du processing de l'événement civil décrit dans la structure donnée
	 * @param info description de l'événement civil à traiter maintenant
	 * @return <code>true</code> si tout s'est bien passé et que l'on peut continuer sur les événements suivants, <code>false</code> si on ne doit pas continuer
	 */
	private boolean processEvent(final EvenementCivilEchBasicInfo info) {
		final ParenteRefreshForcingInterceptor myInterceptor = new ParenteRefreshForcingInterceptor(info.getNoIndividu());
		mainInterceptor.register(myInterceptor);
		try {
			return doInNewTransaction(new TransactionCallback<Boolean>() {
				@Override
				public Boolean doInTransaction(TransactionStatus status) {

					// première chose, on invalide le cache de l'individu (afin que les stratégies aient déjà une version à jour des individus)
					dataEventService.onIndividuChange(info.getNoIndividu());

					final EvenementCivilEch evt = fetchDatabaseEvent(info);
					if (evt.getEtat().isTraite()) {
						LOGGER.info(String.format("Evénement %d déjà dans l'état %s, on ne le re-traite pas", info.getId(), evt.getEtat()));
						return Boolean.TRUE;
					}

					try {
						final List<EvenementCivilEchBasicInfo> sortedReferrers = info.getSortedReferrers();
						if (LOGGER.isDebugEnabled() && sortedReferrers.size() > 0) {
							final StringBuilder b = new StringBuilder();
							b.append("Evénement principal : ").append(evt.getId());
							b.append(", événements dépendants :");
							for (EvenementCivilEchBasicInfo ref : sortedReferrers) {
								b.append(' ').append(ref.getId());
							}
							LOGGER.debug(b.toString());
						}
						final List<EvenementCivilEch> referingEvts = buildEventList(sortedReferrers);
						return processEvent(evt, referingEvts, info.getDate(), info.getIdForDataAfterEvent());
					}
					catch (EvenementCivilException e) {
						throw new EvenementCivilWrappingException(e);
					}
				}
			});
		}
		catch (EvenementCivilWrappingException e) {
			LOGGER.error(String.format("Exception reçue lors du traitement de l'événement %d", info.getId()), e.getCause());
			onException(info, e.getCause());
			return false;
		}
		catch (Exception e) {
			LOGGER.error(String.format("Exception reçue lors du traitement de l'événement %d", info.getId()), e);
			onException(info, e);
			return false;
		}
		finally {
			mainInterceptor.unregister(myInterceptor);
		}
	}

	@NotNull
	private List<EvenementCivilEch> buildEventList(List<EvenementCivilEchBasicInfo> infos) {
		final List<EvenementCivilEch> evts;
		if (infos != null && infos.size() > 0) {
			evts = new ArrayList<>(infos.size());
			for (EvenementCivilEchBasicInfo refInfo : infos) {
				final EvenementCivilEch ref = fetchDatabaseEvent(refInfo);
				evts.add(ref);
			}
		}
		else {
			evts = Collections.emptyList();
		}
		return evts;
	}

	/**
	 * Récupère l'événement civil depuis la DB, et rattrappe éventuellement le numéro d'individu absent
	 * @param info information sur l'événement civil à récupérer
	 * @return événement civil tiré de la DB
	 */
	@NotNull
	private EvenementCivilEch fetchDatabaseEvent(EvenementCivilEchBasicInfo info) {
		final EvenementCivilEch evt = evtCivilDAO.get(info.getId());
		if (evt == null) {
			throw new IllegalArgumentException("Pas d'événement civil trouvé avec le numéro " + info.getId());
		}
		if (evt.getNumeroIndividu() == null) {
			evt.setNumeroIndividu(info.getNoIndividu());
		}
		return evt;
	}

	/**
	 * Assigne le message d'erreur à l'événement en fonction de l'exception
	 * @param info description de l'événement en cours de traitement
	 * @param e exception qui a sauté
	 */
	private void onException(final EvenementCivilEchBasicInfo info, final Exception e) {
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementCivilEchErreur erreur = ERREUR_FACTORY.createErreur(e);
				final EvenementCivilEch evt = fetchDatabaseEvent(info);
				final List<EvenementCivilEch> referrers = buildEventList(info.getSortedReferrers());
				final EvenementCivilGrappe grappe = buildGrappe(evt, referrers);
				grappe.forEach(CLEANUP_AVANT_TRAITEMENT);
				grappe.forEach(DATE_TRAITEMENT);
				evt.getErreurs().add(erreur);
				assignerEtatApresTraitement(EtatEvenementCivil.EN_ERREUR, grappe);
				return null;
			}
		});
	}

	/**
	 * Quand la méthode {@link #processEventAndDoPostProcessingOnError} a renvoyé <code>false</code>, il faut passer tous les événements
	 * restant de la liste de l'état "A_TRAITER" en "EN_ATTENTE"
	 * @param remainingEvents descriptif des événements dans la queue
	 */
	@SuppressWarnings("unchecked")
	private void errorPostProcessing(List<EvenementCivilEchBasicInfo> remainingEvents) {
		if (remainingEvents != null && remainingEvents.size() > 0) {

			// itération sur toutes les stratégies dans l'ordre d'insertion
			List<EvenementCivilEchBasicInfo> currentlyRemaining = remainingEvents;
			for (final ErrorPostProcessingStrategy strategy : postProcessingStrategies) {

				// phase de collecte
				final Mutable<Object> dataHolder = new MutableObject<>();
				if (strategy.needsTransactionOnCollectPhase()) {
					final List<EvenementCivilEchBasicInfo> toAnalyse = currentlyRemaining;
					currentlyRemaining = doInNewTransaction(new TransactionCallback<List<EvenementCivilEchBasicInfo>>() {
						@Override
						public List<EvenementCivilEchBasicInfo> doInTransaction(TransactionStatus status) {
							return strategy.doCollectPhase(toAnalyse, dataHolder);
						}
					});
				}
				else {
					currentlyRemaining = strategy.doCollectPhase(currentlyRemaining, dataHolder);
				}

				// phase de finalisation
				if (strategy.needsTransactionOnFinalizePhase()) {
					doInNewTransaction(new TransactionCallback<Object>() {
						@Override
						public Object doInTransaction(TransactionStatus status) {
							strategy.doFinalizePhase(dataHolder.getValue());
							return null;
						}
					});
				}
				else {
					strategy.doFinalizePhase(dataHolder.getValue());
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

	private interface GroupAction {
		void execute(boolean principal, boolean hasReferrers, EvenementCivilEch evt);
	}

	private static class EvenementCivilGrappe {

		private final EvenementCivilEch eventPrincipal;
		private final List<EvenementCivilEch> referrers;

		private EvenementCivilGrappe(EvenementCivilEch eventPrincipal, List<EvenementCivilEch> referrers) {
			this.eventPrincipal = eventPrincipal;
			this.referrers = referrers;
		}

		public void forEach(GroupAction action) {
			action.execute(true, referrers.size() > 0, eventPrincipal);
			for (EvenementCivilEch ref : referrers) {
				action.execute(false, true, ref);
			}
		}
	}

	private static final GroupAction CLEANUP_AVANT_TRAITEMENT = new GroupAction() {
		@Override
		public void execute(boolean principal, boolean hasReferrers, EvenementCivilEch evt) {
			if (!evt.getEtat().isTraite()) {
				evt.setCommentaireTraitement(null);
				evt.getErreurs().clear();
			}
		}
	};

	private final GroupAction TRAITEMENT_ANNULATION = new GroupAction() {
		@Override
		public void execute(boolean principal, boolean hasReferrers, EvenementCivilEch evt) {
			if (!evt.getEtat().isTraite()) {
				evt.setDateTraitement(DateHelper.getCurrentDate());
				evt.setCommentaireTraitement(COMMENTAIRE_ANNULATION_GROUPEE);
				evt.setEtat(EtatEvenementCivil.REDONDANT);
				audit.info(evt.getId(), String.format("Marquage de l'événement %d (%s/%s) comme redondant (groupe d'événements annulés avant d'avoir été traités)", evt.getId(), evt.getType(), evt.getAction()));
			}
		}
	};

	private static final GroupAction DATE_TRAITEMENT = new GroupAction() {
		@Override
		public void execute(boolean principal, boolean hasReferrers, EvenementCivilEch evt) {
			if (!evt.getEtat().isTraite()) {
				evt.setDateTraitement(DateHelper.getCurrentDate());
			}
		}
	};

	private class CorrectionGrappeAction implements GroupAction {

		private final long idEvtPrincipal;
		private final EtatEvenementCivil etatReferrers;

		private final Set<EtatEvenementCivil> PASSER_EN_ATTENTE_SI_ERREUR = EnumSet.of(EtatEvenementCivil.A_TRAITER, EtatEvenementCivil.EN_ATTENTE);

		private CorrectionGrappeAction(long idEvtPrincipal, EtatEvenementCivil etatReferrers) {
			this.idEvtPrincipal = idEvtPrincipal;
			this.etatReferrers = etatReferrers;
		}

		@Override
		public void execute(boolean principal, boolean hasReferrers, EvenementCivilEch evt) {
			if (principal && hasReferrers) {
				final String commentaireExistant = evt.getCommentaireTraitement();
				final String nouveauCommentaire;
				if (StringUtils.isBlank(commentaireExistant)) {
					nouveauCommentaire = COMMENTAIRE_ACTION_CORRECTIVE_GROUPEE;
				}
				else {
					nouveauCommentaire = StringUtils.abbreviate(String.format("%s %s", commentaireExistant, COMMENTAIRE_ACTION_CORRECTIVE_GROUPEE), LengthConstants.EVTCIVILECH_COMMENT);
				}
				evt.setCommentaireTraitement(nouveauCommentaire);
			}
			else if (!principal && !evt.getEtat().isTraite()) {
				if (etatReferrers == EtatEvenementCivil.EN_ERREUR && PASSER_EN_ATTENTE_SI_ERREUR.contains(evt.getEtat())) {
					evt.setEtat(EtatEvenementCivil.EN_ATTENTE);
				}
				else {
					evt.setEtat(etatReferrers);
				}
				evt.setCommentaireTraitement(COMMENTAIRE_CORRECTION_GROUPEE);
				audit.info(evt.getId(), String.format("Evénement %d (%s/%s) traité (-> %s) avec son événement référencé (%d)", evt.getId(), evt.getType(), evt.getAction(), evt.getEtat(), idEvtPrincipal));
			}
		}
	}

	/**
	 * Appelé dans une transaction pour lancer le traitement de l'événement civil
	 * @param event événement à traiter
	 * @param referrers la liste des événements à traiter en même temps car tous forment un groupe
	 * @param refDate date effective de validité de l'événement (en cas de groupe, cette date peut varier de la date de l'événement principal)
	 * @return <code>true</code> si tout s'est bien passé, <code>false</code> si l'événement a été mis en erreur
	 * @throws ch.vd.unireg.evenement.civil.common.EvenementCivilException en cas de problème métier
	 */
	private boolean processEvent(EvenementCivilEch event, List<EvenementCivilEch> referrers, RegDate refDate, long evtIdForDataAfterEvent) throws EvenementCivilException {
		String dateMove = StringUtils.EMPTY;
		if (refDate != event.getDateEvenement()) {
			dateMove = String.format(" (-> %s)", RegDateHelper.dateToDisplayString(refDate));
		}
		audit.info(event.getId(), String.format("Début du traitement de l'événement civil %d de type %s/%s au %s%s sur l'individu %d",
		                                        event.getId(), event.getType(), event.getAction(),
		                                        RegDateHelper.dateToDisplayString(event.getDateEvenement()), dateMove,
		                                        event.getNumeroIndividu()));

		// construction d'une liste de tous les événements traités ensemble
		final EvenementCivilGrappe grappe = buildGrappe(event, referrers);

		// élimination des erreurs et du commentaire de traitement en cas de retraitement
		grappe.forEach(CLEANUP_AVANT_TRAITEMENT);

		// cas facile où il faut tout annuler
		if (isAnnulationTotale(event, referrers)) {

			grappe.forEach(TRAITEMENT_ANNULATION);

			// dans les cas "redondants", on n'a touché à rien, mais il est peut-être utile de forcer une ré-indexation quand-même, non ?
			scheduleIndexation(event.getNumeroIndividu());
			return true;
		}
		else {
			final EvenementCivilMessageCollector<EvenementCivilEchErreur> collector = new EvenementCivilMessageCollector<>(ERREUR_FACTORY);
			final EvenementCivilEchFacade eventFacade = buildFacade(event, refDate, evtIdForDataAfterEvent);
			final EtatEvenementCivil etat = processEventAndCollectMessages(eventFacade, collector, collector);

			grappe.forEach(DATE_TRAITEMENT);

			// les erreurs et warnings collectés sont maintenant associés à l'événement en base
			final List<EvenementCivilEchErreur> erreurs = EvenementCivilHelper.eliminerDoublons(collector.getErreurs());
			final List<EvenementCivilEchErreur> warnings = EvenementCivilHelper.eliminerDoublons(collector.getWarnings());
			event.getErreurs().addAll(erreurs);
			event.getErreurs().addAll(warnings);

			for (EvenementCivilEchErreur e : erreurs) {
				audit.error(event.getId(), e.getMessage());
			}
			for (EvenementCivilEchErreur w : warnings) {
				audit.warn(event.getId(), w.getMessage());
			}

			final boolean hasErrors = collector.hasErreurs();
			final EtatEvenementCivil etatEffectif = hasErrors ? EtatEvenementCivil.EN_ERREUR : (collector.hasWarnings() ? EtatEvenementCivil.A_VERIFIER : etat);
			assignerEtatApresTraitement(etatEffectif, grappe);

			// dans les cas "redondants", on n'a touché à rien, mais il est peut-être utile de forcer une ré-indexation quand-même, non ?
			if (etatEffectif == EtatEvenementCivil.REDONDANT) {
				scheduleIndexation(event.getNumeroIndividu());
			}

			return !hasErrors;
		}
	}

	private static EvenementCivilGrappe buildGrappe(EvenementCivilEch eventPrincipal, List<EvenementCivilEch> referrers) {
		return new EvenementCivilGrappe(eventPrincipal, referrers);
	}

	private void assignerEtatApresTraitement(EtatEvenementCivil etat, EvenementCivilGrappe grappe) {
		final EvenementCivilEch eventPrincipal = grappe.eventPrincipal;
		eventPrincipal.setEtat(etat);

		final String messageAudit = String.format("Statut de l'événement passé à '%s'", etat);
		if (etat == EtatEvenementCivil.EN_ERREUR) {
			audit.error(eventPrincipal.getId(), messageAudit);
		}
		else if (etat == EtatEvenementCivil.A_VERIFIER) {
			audit.warn(eventPrincipal.getId(), messageAudit);
		}
		else {
			audit.success(eventPrincipal.getId(), messageAudit);
		}

		// tous les éléments du groupe sont passés au même état de traitement avec un commentaire
		// spécifique pour les événements autres que l'événement principal
		grappe.forEach(new CorrectionGrappeAction(eventPrincipal.getId(), etat));
	}

	/**
	 * Dans le cas des événements groupés, la date de l'événement peut avoir été modifiée par une correction ultérieure
	 * @param event l'événement principal du groupe
	 * @param refDate date effective de validité de l'événement (en cas de groupe, cette date peut varier de la date de l'événement principal)
	 * @return une façade qui peut être utilisée pour le traitement du groupe dans son ensemble
	 */
	private static EvenementCivilEchFacade buildFacade(EvenementCivilEch event, final RegDate refDate, final long idEvtForDataAfterEvent) {
		if (refDate == event.getDateEvenement() && event.getId() == idEvtForDataAfterEvent) {
			return event;
		}

		return new EvenementCivilEchWrappingFacade(event) {
			@NotNull
			@Override
			public RegDate getDateEvenement() {
				return refDate;
			}

			@Override
			public Long getIdForDataAfterEvent() {
				return idEvtForDataAfterEvent;
			}
		};
	}

	/**
	 * Les annulations sont un cas spécial : actuellement, d'après les dernières analyses faites vis-à-vis de l'équipe RCPers,
	 * la seule présence d'une annulation dans une chaîne de dépendances signifie en fait l'annulation de l'entièreté de la chaîne
	 * (jusqu'à l'annonce initiale, donc).
	 * <p/>
	 * Dans l'immédiat, on ne va pas traiter ce cas, mais seulement celui où l'annulation pointe directement vers l'élément initial
	 * (j'ai en effet la conviction assez forte que ce raccourci est une décision RCPers-iènne qui ne résistera pas à la confrontation avec les
	 * fournisseurs de logiciels communaux qui auront leur propre interprétation des dépendances dans les eCH-0020...)
	 *
	 * @return <code>true</code> si la collection des <i>referrers</i> est un événement d'annulation non-traité de l'événement principal
	 */
	private static boolean isAnnulationTotale(EvenementCivilEch event, List<EvenementCivilEch> referrers) {
		boolean isAnnulationTotale = false;
		if (referrers.size() == 1) {
			final EvenementCivilEch referrer = referrers.get(0);
			isAnnulationTotale = event.getAction() == ActionEvenementCivilEch.PREMIERE_LIVRAISON
					&& referrer.getAction() == ActionEvenementCivilEch.ANNULATION
					&& !referrer.getEtat().isTraite()
					&& event.getId().equals(referrer.getRefMessageId());
		}
		return isAnnulationTotale;
	}

	private EtatEvenementCivil processEventAndCollectMessages(EvenementCivilEchFacade event, EvenementCivilErreurCollector erreurs, EvenementCivilWarningCollector warnings) throws EvenementCivilException {
		final EvenementCivilInterne evtInterne = buildInterne(event);
		if (evtInterne == null) {
			LOGGER.error(String.format("Aucun code de traitement trouvé pour l'événement %d", event.getId()));
			erreurs.addErreur("Aucun code de traitement trouvé.");
			return EtatEvenementCivil.EN_ERREUR;
		}
		else {
			// validation et traitement
			final EtatEvenementCivil etat;
			evtInterne.validate(erreurs, warnings);
			if (erreurs.hasErreurs()) {
				etat = EtatEvenementCivil.EN_ERREUR;
			}
			else {
				etat = evtInterne.handle(warnings).toEtat();
			}
			if (StringUtils.isNotBlank(event.getCommentaireTraitement()) && evtInterne.shouldResetCommentaireTraitement(etat, event.getCommentaireTraitement())) {
				event.setCommentaireTraitement(null);
			}
			return etat;
		}
	}

	private EvenementCivilInterne buildInterne(EvenementCivilEchFacade event) throws EvenementCivilException {
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
			processor.setDaemon(false);         // attention, ce thread est très actif en écriture, il ne faudrait pas l'arrêter trop brusquement si possible
			processor.start();
		}
	}

	@Override
	public void stop() {
		if (processor != null) {
			processor.stopIt();
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
	public void restartProcessingThread() {
		// arrêt
		stop();

		// démarrage
		start();
	}

	@Override
	public boolean isRunning() {
		return processor != null && processor.isAlive();
	}

	@Override
	public int getPhase() {
		return Integer.MAX_VALUE;   // as late as possible during starting process
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		postProcessingStrategies = new ArrayList<>();
		postProcessingStrategies.add(new ErrorPostProcessingIndexationPureStrategy(evtCivilDAO, translator, this));
		postProcessingStrategies.add(new ErrorPostProcessingMiseEnAttenteStrategy(evtCivilDAO, audit));
	}
}
