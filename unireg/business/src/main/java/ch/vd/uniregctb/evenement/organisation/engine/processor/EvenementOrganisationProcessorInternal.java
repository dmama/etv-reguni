package ch.vd.uniregctb.evenement.organisation.engine.processor;

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
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.uniregctb.audit.Audit;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.data.DataEventService;
import ch.vd.uniregctb.evenement.EvenementCivilHelper;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisation;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationAbortException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationBasicInfo;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationConservationMessagesException;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationDAO;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationErreur;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationErreurFactory;
import ch.vd.uniregctb.evenement.organisation.EvenementOrganisationException;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationErreurCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationMessageCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationSuiviCollector;
import ch.vd.uniregctb.evenement.organisation.audit.EvenementOrganisationWarningCollector;
import ch.vd.uniregctb.evenement.organisation.engine.ErrorPostProcessingMiseEnAttenteStrategy;
import ch.vd.uniregctb.evenement.organisation.engine.ErrorPostProcessingStrategy;
import ch.vd.uniregctb.evenement.organisation.engine.translator.EvenementOrganisationTranslator;
import ch.vd.uniregctb.evenement.organisation.interne.EvenementOrganisationInterne;
import ch.vd.uniregctb.indexer.tiers.GlobalTiersIndexer;
import ch.vd.uniregctb.tiers.Entreprise;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.type.EtatEvenementOrganisation;

/**
 * @author Raphaël Marmier, 2015-07-27
 */
public class EvenementOrganisationProcessorInternal implements ProcessorInternal, InitializingBean {

	public static final String EVT_ORGANISATION_PRINCIPAL = "EvtOrganisation";

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementOrganisationProcessorInternal.class);

	private static final EvenementOrganisationErreurFactory ERREUR_FACTORY = new EvenementOrganisationErreurFactory();

	private PlatformTransactionManager transactionManager;
	private EvenementOrganisationDAO evtOrganisationDAO;
	private EvenementOrganisationTranslator translator;
	private DataEventService dataEventService;

	private GlobalTiersIndexer indexer;
	private TiersService tiersService;

	private List<ErrorPostProcessingStrategy> postProcessingStrategies;

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEvtOrganisationDAO(EvenementOrganisationDAO evtOrganisationDAO) {
		this.evtOrganisationDAO = evtOrganisationDAO;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTranslator(EvenementOrganisationTranslator translator) {
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

	public void setDataEventService(DataEventService dataEventService) {
		this.dataEventService = dataEventService;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean processEventAndDoPostProcessingOnError(EvenementOrganisationBasicInfo evt, List<EvenementOrganisationBasicInfo> evts, int pointer) {
		AuthenticationHelper.pushPrincipal(String.format(EVT_ORGANISATION_PRINCIPAL + "-%d", evt.getNoEvenement()));
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
	public boolean forceEvent(EvenementOrganisationBasicInfo evt) {
		if (evt.getEtat().isTraite() && evt.getEtat() != EtatEvenementOrganisation.A_VERIFIER) {
			throw new IllegalArgumentException("L'état de l'événement " + evt.getId() + " (rcent: " + evt.getNoEvenement() + ") ne lui permet pas d'être forcé");
		}
		return processEvent(evt, true);
	}

	/**
	 * Classe d'exception utilisée pour wrapper une {@link EvenementOrganisationException}
	 */
	final class EvenementOrganisationWrappingException extends RuntimeException {
		private EvenementOrganisationWrappingException(EvenementOrganisationException cause) {
			super(cause);
		}

		@Override
		public EvenementOrganisationException getCause() {
			return (EvenementOrganisationException) super.getCause();
		}
	}

	/**
	 * Lancement du processing de l'événement organisation décrit dans la structure donnée
	 * @param info description de l'événement organisation à traiter maintenant
	 * @return <code>true</code> si tout s'est bien passé et que l'on peut continuer sur les événements suivants, <code>false</code> si on ne doit pas continuer
	 */
	private boolean processEvent(final EvenementOrganisationBasicInfo info, final boolean force) {
		try {
			return doInNewTransaction(new TransactionCallback<Boolean>() {
				@Override
				public Boolean doInTransaction(TransactionStatus status) {

					// première chose, on invalide le cache de l'organisation (afin que les stratégies aient déjà une version à jour de l'organisation)
					dataEventService.onOrganisationChange(info.getNoOrganisation());

					// Detecter les événements déjà traités
					final EvenementOrganisation evt = fetchDatabaseEvent(info);
					if (evt.getEtat().isTraite()) {
						if (!force || info.getEtat() != EtatEvenementOrganisation.A_VERIFIER) {
							LOGGER.info(String.format("Evénement %s (rcent: %d) déjà dans l'état %s, on ne le re-traite pas", info.getId(), info.getNoEvenement(), evt.getEtat()));
							return Boolean.TRUE;
						}
					}

					try {
						return processEvent(evt, force);
					}
					catch (EvenementOrganisationException e) {
						throw new EvenementOrganisationWrappingException(e);
					}
				}
			});
		}
		catch (EvenementOrganisationWrappingException e) {
			LOGGER.error(String.format("Exception reçue lors du traitement de l'événement %d (rcent: %d)", info.getId(), info.getNoEvenement()), e.getCause());
			onException(info, e.getCause(), force);
			return false;
		}
		catch (Exception e) {
			LOGGER.error(String.format("Exception reçue lors du traitement de l'événement %d (rcent: %d)", info.getId(), info.getNoEvenement()), e);
			onException(info, e, force);
			return false;
		}
	}

	/**
	 * Récupère l'événement organisation depuis la DB
	 * @param info information sur l'événement organisation à récupérer
	 * @return événement organisation tiré de la DB
	 */
	@NotNull
	private EvenementOrganisation fetchDatabaseEvent(EvenementOrganisationBasicInfo info) {
		final EvenementOrganisation evt = evtOrganisationDAO.get(info.getId());
		if (evt == null) {
			throw new IllegalArgumentException("Pas d'événement organisation trouvé avec le numéro technique " + info.getId());
		}
		return evt;
	}

	/**
	 * Assigne le message d'erreur à l'événement en fonction de l'exception
	 * @param info description de l'événement en cours de traitement
	 * @param e exception qui a sauté
	 */
	private void onException(final EvenementOrganisationBasicInfo info, final Exception e, final boolean force) {
		doInNewTransaction(new TransactionCallback<Object>() {
			@Override
			public Object doInTransaction(TransactionStatus status) {
				final EvenementOrganisation evt = fetchDatabaseEvent(info);
				if (!force) {
					cleanupAvantTraitement(evt);
				}
				addDateTraitement(evt);

				// c'est un cas spécial, on veut conserver les messages...
				final boolean keepStack;
				if (e instanceof EvenementOrganisationConservationMessagesException) {
					final EvenementOrganisationConservationMessagesException ex = (EvenementOrganisationConservationMessagesException) e;
					final EvenementOrganisationMessageCollector<EvenementOrganisationErreur> messageCollector = ex.getMessageCollector();
					keepStack = ex.keepStack();
					evt.getErreurs().addAll(messageCollector.getEntrees());
				}
				else {
					keepStack = true;
				}

				final EvenementOrganisationErreur erreur = keepStack ? ERREUR_FACTORY.createErreur(e) : ERREUR_FACTORY.createErreur(e.getMessage());
				evt.getErreurs().add(erreur);

				assignerEtatApresTraitement(EtatEvenementOrganisation.EN_ERREUR, evt);
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
	private void errorPostProcessing(List<EvenementOrganisationBasicInfo> remainingEvents) {
		if (remainingEvents != null && remainingEvents.size() > 0) {

			// itération sur toutes les stratégies dans l'ordre d'insertion
			List<EvenementOrganisationBasicInfo> currentlyRemaining = remainingEvents;
			for (final ErrorPostProcessingStrategy strategy : postProcessingStrategies) {

				// phase de collecte
				final Mutable<Object> dataHolder = new MutableObject<>();
				if (strategy.needsTransactionOnCollectPhase()) {
					final List<EvenementOrganisationBasicInfo> toAnalyse = currentlyRemaining;
					currentlyRemaining = doInNewTransaction(new TransactionCallback<List<EvenementOrganisationBasicInfo>>() {
						@Override
						public List<EvenementOrganisationBasicInfo> doInTransaction(TransactionStatus status) {
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
	 * Demande une ré-indexation du tiers lié à l'organisation dont l'identifiant est fourni (doit être appelé dans un
	 * context transactionnel)
	 * @param noOrganisation identifiant d'organisation
	 */
	private void scheduleIndexation(long noOrganisation) {
		final Entreprise pm = tiersService.getEntrepriseByNumeroOrganisation(noOrganisation);
		if (pm != null) {
			indexer.schedule(pm.getNumero());
		}
	}

	private <T> T doInNewTransaction(final TransactionCallback<T> action) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		return template.execute(action);
	}

	private void cleanupAvantTraitement(EvenementOrganisation evt) {
		if (!evt.getEtat().isTraite()) {
			evt.setCommentaireTraitement(null);
			evt.getErreurs().clear();
		}
	}

	private void addDateTraitement(EvenementOrganisation evt) {
		if (!evt.getEtat().isTraite()) {
			evt.setDateTraitement(DateHelper.getCurrentDate());
		}
	}

	/**
	 * Appelé dans une transaction pour lancer le traitement de l'événement organisation
	 * @param event événement à traiter
	 * @param force Force l'événement
	 * @return <code>true</code> si tout s'est bien passé, <code>false</code> si l'événement a été mis en erreur
	 * @throws EvenementOrganisationException en cas de problème métier
	 */
	private boolean processEvent(final EvenementOrganisation event, final boolean force) throws EvenementOrganisationException {
		Audit.info(event.getNoEvenement(), String.format("Début du traitement de l'événement organisation %s, type %s.",
		                                        event.toString(), event.getType()));

		// élimination des erreurs et du commentaire de traitement en cas de retraitement
		if (!force) {
			cleanupAvantTraitement(event);
		}

		final EvenementOrganisationMessageCollector<EvenementOrganisationErreur> collector = new EvenementOrganisationMessageCollector<>(ERREUR_FACTORY);

		// Si on est en forçage, inscrire un message de suivi. Forcer inconditionnellement si c'est un A_VERIFIER car dans ce cas, il n'y a rien de plus à faire.
		if (force) {
			collector.addSuivi("Forçage de l'événement RCEnt.");
		}

		final EtatEvenementOrganisation etat;
		if (force && event.getEtat() == EtatEvenementOrganisation.A_VERIFIER) {
			etat = EtatEvenementOrganisation.FORCE;
		}
		else {
			try {
				etat = processEventAndCollectMessages(event, collector, collector, collector, force);
			}
			catch (EvenementOrganisationAbortException e) {
				// Le traitement a été interrompu volontairement et il faut conserver les messages accumulés jusque là.
				throw new EvenementOrganisationConservationMessagesException(e.getMessage(), collector, false);
			}
		}

		addDateTraitement(event);

		// les erreurs et warnings collectés sont maintenant associés à l'événement en base
		final List<EvenementOrganisationErreur> entrees = EvenementCivilHelper.eliminerDoublons(collector.getEntrees());
		event.getErreurs().addAll(entrees);

		auditErreurs(event, entrees);

		final EtatEvenementOrganisation etatEffectif;

		if (force) {
			etatEffectif = determineEtatEffectifForce(collector, etat);
		} else {
			etatEffectif = determineEtatEffectif(collector, etat);
		}

		assignerEtatApresTraitement(etatEffectif, event);

		// dans les cas "redondants", on n'a touché à rien, mais il est peut-être utile de forcer une ré-indexation quand-même, non ? //
		if (etatEffectif == EtatEvenementOrganisation.REDONDANT) {
			scheduleIndexation(event.getNoOrganisation());
		}

		return etatEffectif != EtatEvenementOrganisation.EN_ERREUR;
	}

	private EtatEvenementOrganisation determineEtatEffectif(EvenementOrganisationMessageCollector<EvenementOrganisationErreur> collector, EtatEvenementOrganisation etat) {
		return collector.hasErreurs() ? EtatEvenementOrganisation.EN_ERREUR : (collector.hasWarnings() ? EtatEvenementOrganisation.A_VERIFIER : etat);
	}

	private EtatEvenementOrganisation determineEtatEffectifForce(EvenementOrganisationMessageCollector<EvenementOrganisationErreur> collector, EtatEvenementOrganisation etat) {
		return collector.hasErreurs() ? EtatEvenementOrganisation.EN_ERREUR : EtatEvenementOrganisation.FORCE;
	}

	private void auditErreurs(EvenementOrganisation event, List<EvenementOrganisationErreur> entrees) {
		for (EvenementOrganisationErreur e : entrees) {
			switch (e.getType()) {
			case ERROR:
				Audit.error(event.getNoEvenement(), e.getMessage());
				break;
			case WARNING:
				Audit.warn(event.getNoEvenement(), e.getMessage());
				break;
			case SUIVI:
				Audit.info(event.getNoEvenement(), e.getMessage());
				break;
			default:
				throw new IllegalArgumentException(String.format("Type d'erreur inconnu: %s", e.getType()));
			}
		}
	}

	private static void assignerEtatApresTraitement(EtatEvenementOrganisation etat, EvenementOrganisation event) {
		event.setEtat(etat);

		final String messageAudit = String.format("Statut de l'événement passé à '%s'", etat);
		if (etat == EtatEvenementOrganisation.EN_ERREUR) {
			Audit.error(event.getNoEvenement(), messageAudit);
		}
		else if (etat == EtatEvenementOrganisation.A_VERIFIER) {
			Audit.warn(event.getNoEvenement(), messageAudit);
		}
		else {
			Audit.success(event.getNoEvenement(), messageAudit);
		}
	}

	private EtatEvenementOrganisation processEventAndCollectMessages(EvenementOrganisation event,
	                                                                 EvenementOrganisationErreurCollector erreurs,
	                                                                 EvenementOrganisationWarningCollector warnings,
	                                                                 EvenementOrganisationSuiviCollector suivis,
	                                                                 boolean force) throws EvenementOrganisationException {
		// Translate event
		final EvenementOrganisationInterne evtInterne = buildInterne(event);
		if (evtInterne == null) {
			LOGGER.error(String.format("Aucun code de traitement trouvé pour l'événement %s", event.toString()));
			erreurs.addErreur("Aucun code de traitement trouvé.");
			return EtatEvenementOrganisation.EN_ERREUR;
		}
		else {
			// Filtrage des événements à traiter
			EvenementOrganisationInterne toProcess;
			if (force) {
				toProcess = evtInterne.seulementEvenementsFiscaux();
				if (toProcess == null) {
					return EtatEvenementOrganisation.FORCE;
				}
			} else {
				toProcess = evtInterne;
			}

			// validation et traitement
			final EtatEvenementOrganisation etat;
			toProcess.validate(erreurs, warnings, suivis);
			if (erreurs.hasErreurs()) {
				etat = EtatEvenementOrganisation.EN_ERREUR;
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

	private EvenementOrganisationInterne buildInterne(EvenementOrganisation event) throws EvenementOrganisationException {
		return translator.toInterne(event);
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		postProcessingStrategies = new ArrayList<>();
		postProcessingStrategies.add(new ErrorPostProcessingMiseEnAttenteStrategy(evtOrganisationDAO));
	}
}
