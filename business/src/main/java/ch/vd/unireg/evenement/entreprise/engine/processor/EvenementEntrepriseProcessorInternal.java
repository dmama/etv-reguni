package ch.vd.unireg.evenement.entreprise.engine.processor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.unireg.audit.AuditManager;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.data.CivilDataEventNotifier;
import ch.vd.unireg.evenement.EvenementCivilHelper;
import ch.vd.unireg.evenement.entreprise.EvenementEntreprise;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseAbortException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseBasicInfo;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseConservationMessagesException;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseDAO;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseErreur;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseErreurFactory;
import ch.vd.unireg.evenement.entreprise.EvenementEntrepriseException;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseErreurCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseMessageCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseSuiviCollector;
import ch.vd.unireg.evenement.entreprise.audit.EvenementEntrepriseWarningCollector;
import ch.vd.unireg.evenement.entreprise.engine.ErrorPostProcessingMiseEnAttenteStrategy;
import ch.vd.unireg.evenement.entreprise.engine.ErrorPostProcessingStrategy;
import ch.vd.unireg.evenement.entreprise.engine.translator.EvenementEntrepriseTranslator;
import ch.vd.unireg.evenement.entreprise.interne.EvenementEntrepriseInterne;
import ch.vd.unireg.indexer.tiers.GlobalTiersIndexer;
import ch.vd.unireg.tiers.Entreprise;
import ch.vd.unireg.tiers.TiersService;
import ch.vd.unireg.type.EtatEvenementEntreprise;

/**
 * @author Raphaël Marmier, 2015-07-27
 */
public class EvenementEntrepriseProcessorInternal implements ProcessorInternal, InitializingBean {

	public static final String EVT_ENTREPRISE_PRINCIPAL = "EvtEntreprise";

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementEntrepriseProcessorInternal.class);

	private static final EvenementEntrepriseErreurFactory ERREUR_FACTORY = new EvenementEntrepriseErreurFactory();

	private PlatformTransactionManager transactionManager;
	private EvenementEntrepriseDAO evtEntrepriseDAO;
	private EvenementEntrepriseTranslator translator;
	private CivilDataEventNotifier civilDataEventNotifier;

	private GlobalTiersIndexer indexer;
	private TiersService tiersService;
	private AuditManager audit;

	private List<ErrorPostProcessingStrategy> postProcessingStrategies;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtEntrepriseDAO(EvenementEntrepriseDAO evtEntrepriseDAO) {
		this.evtEntrepriseDAO = evtEntrepriseDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTranslator(EvenementEntrepriseTranslator translator) {
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

	public void setCivilDataEventNotifier(CivilDataEventNotifier civilDataEventNotifier) {
		this.civilDataEventNotifier = civilDataEventNotifier;
	}

	public void setAudit(AuditManager audit) {
		this.audit = audit;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean processEventAndDoPostProcessingOnError(EvenementEntrepriseBasicInfo evt, List<EvenementEntrepriseBasicInfo> evts, int pointer) {
		AuthenticationHelper.pushPrincipal(String.format("%s-%d", EVT_ENTREPRISE_PRINCIPAL, evt.getNoEvenement()));
		try {
			final boolean success = processEvent(evt, false);
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
	 * {@inheritDoc}
	 */
	public boolean forceEvent(EvenementEntrepriseBasicInfo evt) {
		if (evt.getEtat().isTraite() && evt.getEtat() != EtatEvenementEntreprise.A_VERIFIER) {
			throw new IllegalArgumentException("L'état de l'événement " + evt.getId() + " (rcent: " + evt.getNoEvenement() + ") ne lui permet pas d'être forcé");
		}
		return processEvent(evt, true);
	}

	/**
	 * Classe d'exception utilisée pour wrapper une {@link EvenementEntrepriseException}
	 */
	final class EvenementEntrepriseWrappingException extends RuntimeException {
		private EvenementEntrepriseWrappingException(EvenementEntrepriseException cause) {
			super(cause);
		}

		@Override
		public EvenementEntrepriseException getCause() {
			return (EvenementEntrepriseException) super.getCause();
		}
	}

	/**
	 * Lancement du processing de l'événement entreprise décrit dans la structure donnée
	 * @param info description de l'événement entreprise à traiter maintenant
	 * @return <code>true</code> si tout s'est bien passé et que l'on peut continuer sur les événements suivants, <code>false</code> si on ne doit pas continuer
	 */
	private boolean processEvent(final EvenementEntrepriseBasicInfo info, final boolean force) {
		try {
			return doInNewTransaction(status -> {

				// première chose, on invalide le cache de l'entreprise (afin que les stratégies aient déjà une version à jour de l'entreprise)
				civilDataEventNotifier.notifyEntrepriseChange(info.getNoEntrepriseCivile());

				// Detecter les événements déjà traités
				final EvenementEntreprise evt = fetchDatabaseEvent(info);
				if (evt.getEtat().isTraite()) {
					if (!force || info.getEtat() != EtatEvenementEntreprise.A_VERIFIER) {
						audit.info(evt.getId(), String.format("Evénement %s (rcent: %d) déjà dans l'état %s, on ne le re-traite pas", info.getId(), info.getNoEvenement(), evt.getEtat()));
						return Boolean.TRUE;
					}
				}

				try {
					return processEvent(evt, force);
				}
				catch (EvenementEntrepriseException e) {
					throw new EvenementEntrepriseWrappingException(e);
				}
			});
		}
		catch (EvenementEntrepriseWrappingException e) {
			final String message = String.format("Exception reçue lors du traitement de l'événement %d (rcent: %d)", info.getId(), info.getNoEvenement());
			LOGGER.error(message, e.getCause());
			audit.error(info.getId(), message + " : " + e.getCause().getMessage());
			onException(info, e.getCause(), force);
			return false;
		}
		catch (Exception e) {
			final String message = String.format("Exception reçue lors du traitement de l'événement %d (rcent: %d)", info.getId(), info.getNoEvenement());
			LOGGER.error(message, e);
			audit.error(info.getId(), message + " : " + e.getMessage());
			onException(info, e, force);
			return false;
		}
	}

	/**
	 * Récupère l'événement entreprise depuis la DB
	 * @param info information sur l'événement entreprise à récupérer
	 * @return événement entreprise tiré de la DB
	 */
	@NotNull
	private EvenementEntreprise fetchDatabaseEvent(EvenementEntrepriseBasicInfo info) {
		final EvenementEntreprise evt = evtEntrepriseDAO.get(info.getId());
		if (evt == null) {
			throw new IllegalArgumentException("Pas d'événement entreprise trouvé avec le numéro technique " + info.getId());
		}
		return evt;
	}

	/**
	 * Assigne le message d'erreur à l'événement en fonction de l'exception
	 * @param info description de l'événement en cours de traitement
	 * @param e exception qui a sauté
	 */
	private void onException(final EvenementEntrepriseBasicInfo info, final Exception e, final boolean force) {
		doInNewTransaction(status -> {
			final EvenementEntreprise evt = fetchDatabaseEvent(info);
			if (!force) {
				cleanupAvantTraitement(evt);
			}
			addDateTraitement(evt);

			// c'est un cas spécial, on veut conserver les messages...
			final boolean keepStack;
			if (e instanceof EvenementEntrepriseConservationMessagesException) {
				final EvenementEntrepriseConservationMessagesException ex = (EvenementEntrepriseConservationMessagesException) e;
				final EvenementEntrepriseMessageCollector<EvenementEntrepriseErreur> messageCollector = ex.getMessageCollector();
				keepStack = ex.keepStack();
				evt.getErreurs().addAll(messageCollector.getEntrees());
			}
			else {
				keepStack = true;
			}

			final EvenementEntrepriseErreur erreur = keepStack ? ERREUR_FACTORY.createErreur(e) : ERREUR_FACTORY.createErreur(e.getMessage());
			evt.getErreurs().add(erreur);

			assignerEtatApresTraitement(EtatEvenementEntreprise.EN_ERREUR, evt);
			return null;
		});
	}

	/**
	 * Quand la méthode {@link #processEventAndDoPostProcessingOnError} a renvoyé <code>false</code>, il faut passer tous les événements
	 * restant de la liste de l'état "A_TRAITER" en "EN_ATTENTE"
	 * @param remainingEvents descriptif des événements dans la queue
	 */
	@SuppressWarnings("unchecked")
	private void errorPostProcessing(List<EvenementEntrepriseBasicInfo> remainingEvents) {
		if (remainingEvents != null && remainingEvents.size() > 0) {

			// itération sur toutes les stratégies dans l'ordre d'insertion
			List<EvenementEntrepriseBasicInfo> currentlyRemaining = remainingEvents;
			for (final ErrorPostProcessingStrategy strategy : postProcessingStrategies) {

				// phase de collecte
				final Mutable<Object> dataHolder = new MutableObject<>();
				if (strategy.needsTransactionOnCollectPhase()) {
					final List<EvenementEntrepriseBasicInfo> toAnalyse = currentlyRemaining;
					currentlyRemaining = doInNewTransaction(status -> strategy.doCollectPhase(toAnalyse, dataHolder));
				}
				else {
					currentlyRemaining = strategy.doCollectPhase(currentlyRemaining, dataHolder);
				}

				// phase de finalisation
				if (strategy.needsTransactionOnFinalizePhase()) {
					doInNewTransaction(status -> {
						strategy.doFinalizePhase(dataHolder.getValue());
						return null;
					});
				}
				else {
					strategy.doFinalizePhase(dataHolder.getValue());
				}
			}
		}
	}

	/**
	 * Demande une ré-indexation du tiers lié à l'entreprise dont l'identifiant est fourni (doit être appelé dans un
	 * context transactionnel)
	 * @param noEntrepriseCivile identifiant d'entreprise
	 */
	private void scheduleIndexation(long noEntrepriseCivile) {
		final Entreprise pm = tiersService.getEntrepriseByNoEntrepriseCivile(noEntrepriseCivile);
		if (pm != null) {
			indexer.schedule(pm.getNumero());
		}
	}

	private <T> T doInNewTransaction(final TransactionCallback<T> action) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(action);
	}

	private void cleanupAvantTraitement(EvenementEntreprise evt) {
		if (!evt.getEtat().isTraite()) {
			evt.setCommentaireTraitement(null);
			evt.getErreurs().clear();
		}
	}

	private void addDateTraitement(EvenementEntreprise evt) {
		if (!evt.getEtat().isTraite()) {
			evt.setDateTraitement(DateHelper.getCurrentDate());
		}
	}

	/**
	 * Appelé dans une transaction pour lancer le traitement de l'événement entreprise
	 * @param event événement à traiter
	 * @param force Force l'événement
	 * @return <code>true</code> si tout s'est bien passé, <code>false</code> si l'événement a été mis en erreur
	 * @throws EvenementEntrepriseException en cas de problème métier
	 */
	private boolean processEvent(final EvenementEntreprise event, final boolean force) throws EvenementEntrepriseException {
		audit.info(event.getNoEvenement(), String.format("Début du traitement de l'événement entreprise %s, type %s.",
		                                        event.toString(), event.getType()));

		// élimination des erreurs et du commentaire de traitement en cas de retraitement
		if (!force) {
			cleanupAvantTraitement(event);
		}

		final EvenementEntrepriseMessageCollector<EvenementEntrepriseErreur> collector = new EvenementEntrepriseMessageCollector<>(ERREUR_FACTORY);

		// Si on est en forçage, inscrire un message de suivi. Forcer inconditionnellement si c'est un A_VERIFIER car dans ce cas, il n'y a rien de plus à faire.
		if (force) {
			collector.addSuivi("Forçage de l'événement RCEnt.");
		}

		final EtatEvenementEntreprise etat;
		if (force && event.getEtat() == EtatEvenementEntreprise.A_VERIFIER) {
			etat = EtatEvenementEntreprise.FORCE;
		}
		else {
			try {
				etat = processEventAndCollectMessages(event, collector, collector, collector, force);
			}
			catch (EvenementEntrepriseAbortException e) {
				// Le traitement a été interrompu volontairement et il faut conserver les messages accumulés jusque là.
				throw new EvenementEntrepriseConservationMessagesException(e.getMessage(), collector, false);
			}
		}

		addDateTraitement(event);

		// les erreurs et warnings collectés sont maintenant associés à l'événement en base
		final List<EvenementEntrepriseErreur> entrees = EvenementCivilHelper.eliminerDoublons(collector.getEntrees());
		event.getErreurs().addAll(entrees);

		auditErreurs(event, entrees);

		final EtatEvenementEntreprise etatEffectif;

		if (force) {
			etatEffectif = determineEtatEffectifForce(collector, etat);
		} else {
			etatEffectif = determineEtatEffectif(collector, etat);
		}

		assignerEtatApresTraitement(etatEffectif, event);

		// dans les cas "redondants", on n'a touché à rien, mais il est peut-être utile de forcer une ré-indexation quand-même, non ? //
		if (etatEffectif == EtatEvenementEntreprise.REDONDANT) {
			scheduleIndexation(event.getNoEntrepriseCivile());
		}

		return etatEffectif != EtatEvenementEntreprise.EN_ERREUR;
	}

	private EtatEvenementEntreprise determineEtatEffectif(EvenementEntrepriseMessageCollector<EvenementEntrepriseErreur> collector, EtatEvenementEntreprise etat) {
		return collector.hasErreurs() ? EtatEvenementEntreprise.EN_ERREUR : (collector.hasWarnings() ? EtatEvenementEntreprise.A_VERIFIER : etat);
	}

	private EtatEvenementEntreprise determineEtatEffectifForce(EvenementEntrepriseMessageCollector<EvenementEntrepriseErreur> collector, EtatEvenementEntreprise etat) {
		return collector.hasErreurs() ? EtatEvenementEntreprise.EN_ERREUR : EtatEvenementEntreprise.FORCE;
	}

	private void auditErreurs(EvenementEntreprise event, List<EvenementEntrepriseErreur> entrees) {
		for (EvenementEntrepriseErreur e : entrees) {
			switch (e.getType()) {
			case ERROR:
				audit.error(event.getNoEvenement(), e.getMessage());
				break;
			case WARNING:
				audit.warn(event.getNoEvenement(), e.getMessage());
				break;
			case SUIVI:
				audit.info(event.getNoEvenement(), e.getMessage());
				break;
			default:
				throw new IllegalArgumentException(String.format("Type d'erreur inconnu: %s", e.getType()));
			}
		}
	}

	private void assignerEtatApresTraitement(EtatEvenementEntreprise etat, EvenementEntreprise event) {
		event.setEtat(etat);

		final String messageAudit = String.format("Statut de l'événement passé à '%s'", etat);
		if (etat == EtatEvenementEntreprise.EN_ERREUR) {
			audit.error(event.getNoEvenement(), messageAudit);
		}
		else if (etat == EtatEvenementEntreprise.A_VERIFIER) {
			audit.warn(event.getNoEvenement(), messageAudit);
		}
		else {
			audit.success(event.getNoEvenement(), messageAudit);
		}
	}

	private EtatEvenementEntreprise processEventAndCollectMessages(EvenementEntreprise event,
	                                                               EvenementEntrepriseErreurCollector erreurs,
	                                                               EvenementEntrepriseWarningCollector warnings,
	                                                               EvenementEntrepriseSuiviCollector suivis,
	                                                               boolean force) throws EvenementEntrepriseException {
		// Translate event
		final EvenementEntrepriseInterne evtInterne = buildInterne(event);
		if (evtInterne == null) {
			audit.error(event.getId(), String.format("Aucun code de traitement trouvé pour l'événement %s", event.toString()));
			erreurs.addErreur("Aucun code de traitement trouvé.");
			return EtatEvenementEntreprise.EN_ERREUR;
		}
		else {
			// Filtrage des événements à traiter
			EvenementEntrepriseInterne toProcess;
			if (force) {
				toProcess = evtInterne.seulementEvenementsFiscaux();
				if (toProcess == null) {
					return EtatEvenementEntreprise.FORCE;
				}
			} else {
				toProcess = evtInterne;
			}

			// validation et traitement
			final EtatEvenementEntreprise etat;
			toProcess.validate(erreurs, warnings, suivis);
			if (erreurs.hasErreurs()) {
				etat = EtatEvenementEntreprise.EN_ERREUR;
			}
			else {
				etat = toProcess.handle(warnings, suivis).toEtat();
			}
			if (StringUtils.isNotBlank(event.getCommentaireTraitement()) && toProcess.shouldResetCommentaireTraitement(etat, event.getCommentaireTraitement())) {
				event.setCommentaireTraitement(null);
			}
			return etat;
		}
	}

	private EvenementEntrepriseInterne buildInterne(EvenementEntreprise event) throws EvenementEntrepriseException {
		return translator.toInterne(event);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		postProcessingStrategies = new ArrayList<>();
		postProcessingStrategies.add(new ErrorPostProcessingMiseEnAttenteStrategy(evtEntrepriseDAO, audit));
	}
}
