package ch.vd.uniregctb.evenement.reqdes.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Localite;
import ch.vd.unireg.interfaces.infra.data.Pays;
import ch.vd.uniregctb.adresse.AdresseEnvoi;
import ch.vd.uniregctb.adresse.AdresseEtrangere;
import ch.vd.uniregctb.adresse.AdresseException;
import ch.vd.uniregctb.adresse.AdresseGenerique;
import ch.vd.uniregctb.adresse.AdresseService;
import ch.vd.uniregctb.adresse.AdresseSuisse;
import ch.vd.uniregctb.adresse.AdresseSupplementaire;
import ch.vd.uniregctb.adresse.AdresseSupplementaireAdapter;
import ch.vd.uniregctb.adresse.AdresseTiers;
import ch.vd.uniregctb.adresse.TypeAdresseFiscale;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BlockingQueuePollingThread;
import ch.vd.uniregctb.common.Dated;
import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.common.StringRenderer;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.metier.MetierService;
import ch.vd.uniregctb.metier.MetierServiceException;
import ch.vd.uniregctb.metier.assujettissement.Assujettissement;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementException;
import ch.vd.uniregctb.metier.assujettissement.AssujettissementService;
import ch.vd.uniregctb.metier.assujettissement.TypeAssujettissement;
import ch.vd.uniregctb.reqdes.ErreurTraitement;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.reqdes.EvenementReqDes;
import ch.vd.uniregctb.reqdes.InformationsActeur;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.RolePartiePrenante;
import ch.vd.uniregctb.reqdes.TypeInscription;
import ch.vd.uniregctb.reqdes.TypeRole;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.reqdes.UniteTraitementDAO;
import ch.vd.uniregctb.tiers.AppartenanceMenage;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.ContribuableImpositionPersonnesPhysiques;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.ForFiscalPrincipalPP;
import ch.vd.uniregctb.tiers.ForFiscalSecondaire;
import ch.vd.uniregctb.tiers.ForsParTypeAt;
import ch.vd.uniregctb.tiers.MenageCommun;
import ch.vd.uniregctb.tiers.OriginePersonnePhysique;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.RapportEntreTiers;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.tiers.SituationFamillePersonnePhysique;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.GenreImpot;
import ch.vd.uniregctb.type.ModeImposition;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Traitement des unités de traitement issues des événements ReqDes
 */
public class EvenementReqDesProcessorImpl implements EvenementReqDesProcessor, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(EvenementReqDesProcessorImpl.class);

	private static final Set<EtatTraitement> ETATS_FINAUX = EnumSet.of(EtatTraitement.FORCE, EtatTraitement.TRAITE);

	private final BlockingQueue<QueueElement> queue = new LinkedBlockingQueue<>();

	private WorkerThread workerThread;
	private final Map<Long, Listener> listeners = new LinkedHashMap<>();      // pour les tests, c'est pratique de conserver l'ordre (pour le reste, cela ne fait pas de mal...)

	private PlatformTransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private UniteTraitementDAO uniteTraitementDAO;
	private ServiceInfrastructureService infraService;
	private TiersService tiersService;
	private AdresseService adresseService;
	private AssujettissementService assujettissementService;
	private MetierService metierService;

	private static final AtomicLong SEQUENCE = new AtomicLong(0L);

	/**
	 * Handle utilisé pour les listeners de traitement
	 */
	private class HandleImpl implements ListenerHandle {
		private final long id;

		private HandleImpl() {
			id = SEQUENCE.getAndIncrement();
		}

		@Override
		public void unregister() {
			synchronized (listeners) {
				if (listeners.remove(id) == null) {
					throw new IllegalStateException("Unknown - or already unregistered - handle");
				}
			}
		}
	}

	/**
	 * Classe des éléments dans la queue de synchronisation (on conserve leur timestamp
	 * de création pour éventuellement pouvoir faire des statistiques de monitoring)
	 */
	private static final class QueueElement implements Dated {

		private final long startTimestamp;
		private final long idUniteTraitement;

		public QueueElement(long idUniteTraitement) {
			this.startTimestamp = getTimestamp();
			this.idUniteTraitement = idUniteTraitement;
		}

		@Override
		public long getAge(@NotNull TimeUnit unit) {
			final long now = getTimestamp();
			return unit.convert(now - startTimestamp, TimeUnit.NANOSECONDS);
		}
	}

	/**
	 * Thread d'écoute sur la queue, et donc véritable thread de traitement
	 */
	private final class WorkerThread extends BlockingQueuePollingThread<QueueElement> {

		private WorkerThread() {
			super("EvtReqDes", queue);
		}

		@Override
		protected void processElement(@NotNull QueueElement element) {
			processUniteTraitement(element.idUniteTraitement);
		}

		@Override
		protected void onElementProcessed(@NotNull QueueElement element, @Nullable Throwable t) {
			super.onElementProcessed(element, t);
			notifyTraitementUnite(element.idUniteTraitement);
		}

		@Override
		protected void onStop() {
			notifyStopTraitement();
			super.onStop();
		}
	}

	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setUniteTraitementDAO(UniteTraitementDAO uniteTraitementDAO) {
		this.uniteTraitementDAO = uniteTraitementDAO;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	public void setTiersService(TiersService tiersService) {
		this.tiersService = tiersService;
	}

	public void setAdresseService(AdresseService adresseService) {
		this.adresseService = adresseService;
	}

	public void setAssujettissementService(AssujettissementService assujettissementService) {
		this.assujettissementService = assujettissementService;
	}

	public void setMetierService(MetierService metierService) {
		this.metierService = metierService;
	}

	/**
	 * @return un timestamp en nano-secondes
	 */
	private static long getTimestamp() {
		return System.nanoTime();
	}

	@NotNull
	@Override
	public ListenerHandle registerListener(Listener listener) {
		if (listener == null) {
			throw new NullPointerException("listener");
		}

		final HandleImpl handle = new HandleImpl();
		synchronized (listeners) {
			listeners.put(handle.id, listener);
		}
		return handle;
	}

	/**
	 * Appelé par le thread de traitement à chaque traitement terminé
	 * @param idUniteTraitement identifiant de l'unité traitée
	 */
	private void notifyTraitementUnite(long idUniteTraitement) {
		synchronized (listeners) {
			if (!listeners.isEmpty()) {
				for (Listener listener : listeners.values()) {
					try {
						listener.onUniteTraitee(idUniteTraitement);
					}
					catch (Exception e) {
						// pas grave...
					}
				}
			}
		}
	}

	/**
	 * Appelé par le thread de traitement avant de s'arrêter
	 */
	private void notifyStopTraitement() {
		synchronized (listeners) {
			if (!listeners.isEmpty()) {
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

	@Override
	public void postUniteTraitement(long id) {
		queue.add(new QueueElement(id));
	}

	@Override
	public void postUnitesTraitement(Collection<Long> ids) {
		if (ids != null && !ids.isEmpty()) {
			// élimination des doublons et découplage des collections (en cas de manipulation - sur le thread de traitement, par exemple - pendant l'insertion)
			// tout en conservant l'ordre initial (pour les tests)
			for (Long id : new LinkedHashSet<>(ids)) {
				queue.add(new QueueElement(id));
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		workerThread = new WorkerThread();
		workerThread.start();
	}

	@Override
	public void destroy() throws Exception {
		if (workerThread != null && workerThread.isAlive()) {
			workerThread.stopIt();
			workerThread.join();
			workerThread = null;
		}
	}

	private static class ExceptionReqDesWrappingException extends RuntimeException {
		private ExceptionReqDesWrappingException(EvenementReqDesException cause) {
			super(cause);
		}

		@Override
		public EvenementReqDesException getCause() {
			return (EvenementReqDesException) super.getCause();
		}
	}

	private void processUniteTraitement(final long idUniteTraitement) {
		AuthenticationHelper.pushPrincipal(String.format("ReqDes-UT-%d", idUniteTraitement));
		try {
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("Début du traitement de l'unité de traitement %d", idUniteTraitement));
			}
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			template.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final UniteTraitement ut = uniteTraitementDAO.get(idUniteTraitement);

					// si l'unité de traitement est déjà dans un état final, on ne fait rien ici
					if (ETATS_FINAUX.contains(ut.getEtat())) {
						if (LOGGER.isInfoEnabled()) {
							LOGGER.info(String.format("Unité de traitement %d déjà dans un état final (%s) ; aucun traitement supplémentaire n'est donc entrepris ici.", ut.getId(), ut.getEtat()));
						}
						return;
					}

					// au boulot !
					final String visaPrincipal = extractVisaPrincipal(ut.getEvenement());
					AuthenticationHelper.pushPrincipal(String.format("%s-reqdes", visaPrincipal));
					try {
						final Set<ErreurTraitement> erreurs = ut.getErreurs();
						erreurs.clear();

						final MessageCollector errorCollector = new MessageCollector(ErreurTraitement.TypeErreur.ERROR);
						final MessageCollector warningCollector = new MessageCollector(ErreurTraitement.TypeErreur.WARNING);

						processUniteTraitement(ut, errorCollector, warningCollector);         // <-- c'est ici que tout est fait

						// récupération des erreurs, log...
						if (errorCollector.hasCollectedMessages() || warningCollector.hasCollectedMessages()) {
							final List<ErrorInfo> infos = new LinkedList<>();
							infos.addAll(errorCollector.getCollectedMessages());
							infos.addAll(warningCollector.getCollectedMessages());

							final StringBuilder b = new StringBuilder("Message(s) signalé(s) lors du traitement de l'unité de traitement ").append(ut.getId()).append(" :");

							// recopie des erreurs collectées dans l'unité de traitement
							for (ErrorInfo info : infos) {
								b.append("\n- <").append(info.typeErreur).append("> ").append(info.message);
								erreurs.add(new ErreurTraitement(info.typeErreur, info.message));
							}

							if (errorCollector.hasCollectedMessages()) {
								LOGGER.error(b.toString());
							}
							else {
								LOGGER.warn(b.toString());
							}
						}

						final EtatTraitement nouvelEtat = errorCollector.hasCollectedMessages() ? EtatTraitement.EN_ERREUR : EtatTraitement.TRAITE;
						ut.setDateTraitement(DateHelper.getCurrentDate());
						ut.setEtat(nouvelEtat);

						// un petit flush avant de partir (histoire que les visas utilisés soient les bons)
						hibernateTemplate.flush();

						// log après le flush pour que les éventuels problèmes de validation soient pris en compte
						if (LOGGER.isInfoEnabled()) {
							LOGGER.info(String.format("Traitement de l'unité de traitement %d terminé dans l'état %s", idUniteTraitement, nouvelEtat));
						}
					}
					catch (EvenementReqDesException e) {
						throw new ExceptionReqDesWrappingException(e);
					}
					finally {
						AuthenticationHelper.popPrincipal();
					}
				}
			});
		}
		catch (Exception ex) {
			final Exception e;
			if (ex instanceof ExceptionReqDesWrappingException) {
				e = ((ExceptionReqDesWrappingException) ex).getCause();
			}
			else {
				e = ex;
			}

			// un peu de log
			LOGGER.error(String.format("Exception lors du traitement de l'unité de traitement %d", idUniteTraitement), e);

			// le traitement a explosé dans la transaction -> rollback et mise en erreur de l'unité de traitement
			final TransactionTemplate template = new TransactionTemplate(transactionManager);
			template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
			template.execute(new TransactionCallbackWithoutResult() {
				@Override
				protected void doInTransactionWithoutResult(TransactionStatus status) {
					final UniteTraitement ut = uniteTraitementDAO.get(idUniteTraitement);
					ut.setDateTraitement(DateHelper.getCurrentDate());
					ut.setEtat(EtatTraitement.EN_ERREUR);

					final ErreurTraitement erreur = new ErreurTraitement();
					erreur.setCallstack(ExceptionUtils.extractCallStack(e));
					erreur.setMessage(e.getMessage());
					erreur.setType(ErreurTraitement.TypeErreur.ERROR);

					final Set<ErreurTraitement> erreurs = ut.getErreurs();
					erreurs.clear();
					erreurs.add(erreur);
				}
			});

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("Traitement de l'unité de traitement %d terminé dans l'état %s", idUniteTraitement, EtatTraitement.EN_ERREUR));
			}
		}
		finally {
			AuthenticationHelper.popPrincipal();
		}
	}

	private static String extractVisaPrincipal(EvenementReqDes evt) {
		final InformationsActeur acteur = evt.getOperateur() != null ? evt.getOperateur() : evt.getNotaire();
		return acteur.getVisa();
	}

	private void processUniteTraitement(UniteTraitement ut, MessageCollector errorCollector, MessageCollector warningCollector) throws EvenementReqDesException {
		// Deux étapes :
		// 1. étape de contrôle : l'idée est que l'on ne modifie rien en base dans cette phase, et que les erreurs sont remontées dans une liste
		// 2. si la première étape est passée sans erreur, on peut commencer les modifications -> toute erreur levée à ce stade sera l'objet d'une exception
		doControlesPreliminaires(ut, errorCollector, warningCollector);
		if (!errorCollector.hasCollectedMessages()) {
			// on continue
			doProcessing(ut.getEvenement(), ut.getPartiesPrenantes(), warningCollector);
		}
	}

	/**
	 * Classe interne d'aide à la récolte des erreurs (pour dé-doublonner les messages identiques)
	 */
	private static final class ErrorInfo {

		private final ErreurTraitement.TypeErreur typeErreur;
		private final String message;

		private ErrorInfo(ErreurTraitement.TypeErreur typeErreur, String message) {
			this.typeErreur = typeErreur;
			this.message = message;
			if (typeErreur == null || StringUtils.isBlank(message)) {
				throw new IllegalArgumentException("typeErreur et message doivent être renseignés.");
			}
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final ErrorInfo errorInfo = (ErrorInfo) o;
			return message.equals(errorInfo.message) && typeErreur == errorInfo.typeErreur;
		}

		@Override
		public int hashCode() {
			int result = typeErreur.hashCode();
			result = 31 * result + message.hashCode();
			return result;
		}
	}

	/**
	 * Collecteur de messages de traitement (pour séparer les endroits d'où on peut envoyer des erreurs des endroits d'où on ne peut envoyer que des <i>warnings</i>)
	 */
	private static final class MessageCollector {

		private final ErreurTraitement.TypeErreur typeMessage;
		private final Set<ErrorInfo> messages = new HashSet<>();        // comme ça, on évite les doublons

		public MessageCollector(ErreurTraitement.TypeErreur typeMessage) {
			this.typeMessage = typeMessage;
		}

		public void addNewMessage(String message) {
			messages.add(new ErrorInfo(typeMessage, message));
		}

		public boolean hasCollectedMessages() {
			return !messages.isEmpty();
		}

		public Set<ErrorInfo> getCollectedMessages() {
			return messages;
		}
	}

	/**
	 * Effectue les contrôles préliminaires sur l'état des données entrantes, en mettant les erreurs/warnings dans l'unité de traitement directement si nécessaire
	 * @param ut unité de traitement à contrôler
	 * @param errorCollector collecteur d'erreurs (s'il y en a, le traitement s'arrêtera là)
	 * @param warningCollector collecteur de <i>warnings</i> (même s'il y en a, le traitement continue)
	 * @throws EvenementReqDesException s'il vaut mieux tout arrêter tant la situation est grave
	 */
	private void doControlesPreliminaires(UniteTraitement ut, MessageCollector errorCollector, MessageCollector warningCollector) throws EvenementReqDesException {
		final EvenementReqDes evenement = ut.getEvenement();
		final RegDate dateActe = evenement.getDateActe();

		// est-ce un doublon reçu, qui justifie d'un traitement manuel systématique ?
		if (evenement.isDoublon()) {
			errorCollector.addNewMessage(String.format("Un événement correspondant au même acte (numéro d'affaire %s/%s) a déjà été reçu auparavant, celui-ci passe donc en traitement manuel.",
			                                           evenement.getNotaire().getVisa(), evenement.getNumeroMinute()));
		}

		// vérification de la date de l'acte, qui ne doit pas être dans le futur
		final RegDate today = RegDate.get();
		if (today.isBefore(dateActe)) {
			errorCollector.addNewMessage(String.format("La date de l'acte (%s) est dans le futur.", RegDateHelper.dateToDisplayString(dateActe)));
		}

		final Set<EtatCivil> etatsCivilsSansConjoint = EnumSet.of(EtatCivil.CELIBATAIRE, EtatCivil.VEUF, EtatCivil.DIVORCE, EtatCivil.NON_MARIE,
		                                                          EtatCivil.PARTENARIAT_DISSOUS_DECES, EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT);

		// [SIFISC-13278] S'il y a plusieurs sources différentes (civile, création, fiscale) parmi les parties prenantes et que l'une d'entre elles est "civile",
		// alors on passe en traitement manuel
		final Set<PartiePrenante> partiesPrenantes = ut.getPartiesPrenantes();
		if (partiesPrenantes.size() > 1) {
			boolean hasSourceCivile = false;
			boolean hasSourceFiscale = false;
			boolean hasSourceCreation = false;
			for (PartiePrenante pp : partiesPrenantes) {
				if (pp.isSourceCivile()) {
					hasSourceCivile = true;
				}
				else if (pp.getNumeroContribuable() != null) {
					hasSourceFiscale = true;
				}
				else {
					hasSourceCreation = true;
				}
			}
			if (hasSourceCivile && (hasSourceFiscale || hasSourceCreation)) {
				errorCollector.addNewMessage("Parties prenantes en couple avec divergence des sources de données, dont l'une est civile.");
			}
		}

		for (PartiePrenante pp : partiesPrenantes) {
			final Integer ofsCommuneDomicile = pp.getOfsCommune();
			if (ofsCommuneDomicile != null) {
				// problématique des fractions (-> les fors ne peuvent pas être ouverts sur les communes faîtières de fractions)
				final Commune commune = infraService.getCommuneByNumeroOfs(ofsCommuneDomicile, dateActe);

				// [SIFISC-18033] si la source n'est pas civile, il faut vérifier que la commune n'est pas une commune faîtière
				checkCommune(ofsCommuneDomicile, dateActe, commune, errorCollector, !pp.isSourceCivile());

				// [SIFISC-14606] La commune peut être vaudoise si la source est civile
				// la commune de résidence ne doit pas être vaudoise pour une mise à jour automatique
				if (commune != null && commune.isVaudoise() && !pp.isSourceCivile()) {
					errorCollector.addNewMessage(String.format("La commune de résidence (%s/%d) est vaudoise.", commune.getNomOfficiel(), ofsCommuneDomicile));
				}

				// [SIFISC-14606] si la source est civile, la commune doit être vaudoise
				if (commune != null && !commune.isVaudoise() && pp.isSourceCivile()) {
					errorCollector.addNewMessage(String.format("La commune de résidence (%s/%d) n'est pas vaudoise (%s) alors que la source indiquée est civile.",
					                                           commune.getNomOfficiel(), ofsCommuneDomicile, commune.getSigleCanton()));
				}
			}
			else {
				// dans ce cas, le pays est obligatoire et ne peut pas être la Suisse
				final Integer ofsPays = pp.getOfsPays();
				if (ofsPays != null && ofsPays == ServiceInfrastructureService.noOfsSuisse) {
					errorCollector.addNewMessage("Le pays de résidence ne peut pas être la Suisse en l'absence de commune de résidence.");
				}

				// [SIFISC-14606] si la source est civile, l'adresse de résidence à l'étranger doit être refusée
				if (ofsPays != null && ofsPays != ServiceInfrastructureService.noOfsSuisse && pp.isSourceCivile()) {
					errorCollector.addNewMessage(String.format("Le pays de résidence (%d) est hors-Suisse alors que la source indiquée est civile.", ofsPays));
				}
			}

			// problématique des fractions (-> les fors ne peuvent pas être ouverts sur les communes faîtières de fractions)
			for (RolePartiePrenante role : pp.getRoles()) {
				final int ofsCommuneImmeuble = role.getTransaction().getOfsCommune();
				final Commune commune = infraService.getCommuneByNumeroOfs(ofsCommuneImmeuble, dateActe);
				checkCommune(ofsCommuneImmeuble, dateActe, commune, errorCollector, isRoleAcquereurPropriete(role) && !pp.isSourceCivile());
			}

			final EtatCivil etatCivil = pp.getEtatCivil();
			if (etatsCivilsSansConjoint.contains(etatCivil)) {

				// vérification qu'une partie prenante célibataire (ou assimilée comme telle) n'est pas indiquée avec un conjoint
				if (pp.getConjointPartiePrenante() != null || StringUtils.isNotBlank(pp.getNomConjoint()) || StringUtils.isNotBlank(pp.getPrenomConjoint())) {
					errorCollector.addNewMessage(String.format("Incohérence entre l'état civil (%s) et la présence d'un conjoint.", etatCivil.format()));
				}

				// vérification qu'aucune date de séparation n'est indiquée pour ces états civils
				if (pp.getDateSeparation() != null) {
					errorCollector.addNewMessage(String.format("Date de séparation (%s) indiquée incohérente avec l'état civil (%s)", RegDateHelper.dateToDisplayString(pp.getDateSeparation()), etatCivil.format()));
				}
			}

			// aucune date ne doit être dans le futur!
			if (pp.getDateNaissance() != null && today.isBefore(pp.getDateNaissance())) {
				errorCollector.addNewMessage(String.format("Date de naissance fournie dans le futur (%s)", RegDateHelper.dateToDisplayString(pp.getDateNaissance())));
			}
			if (pp.getDateEtatCivil() != null && today.isBefore(pp.getDateEtatCivil())) {
				errorCollector.addNewMessage(String.format("Date d'état civil fournie dans le futur (%s)", RegDateHelper.dateToDisplayString(pp.getDateEtatCivil())));
			}
			if (pp.getDateSeparation() != null && today.isBefore(pp.getDateSeparation())) {
				errorCollector.addNewMessage(String.format("Date de séparation fournie dans le futur (%s)", RegDateHelper.dateToDisplayString(pp.getDateSeparation())));
			}
			if (pp.getDateDeces() != null && today.isBefore(pp.getDateDeces())) {
				errorCollector.addNewMessage(String.format("Date de décès fournie dans le futur (%s)", RegDateHelper.dateToDisplayString(pp.getDateDeces())));
			}
			if (pp.getDateSeparation() != null && pp.getDateEtatCivil() != null && pp.getDateSeparation().isBefore(pp.getDateEtatCivil())) {
				errorCollector.addNewMessage(String.format("La date de séparation (%s) devrait être après la date de dernier état civil (%s)",
				                                           RegDateHelper.dateToDisplayString(pp.getDateSeparation()),
				                                           RegDateHelper.dateToDisplayString(pp.getDateEtatCivil())));
			}

			// vérification que les liens sont bien bi-directionnels
			if (pp.getConjointPartiePrenante() != null && pp.getConjointPartiePrenante().getConjointPartiePrenante() != pp) {
				errorCollector.addNewMessage("Liens matrimoniaux incohérents entres les parties prenantes.");
			}

			// vérification que le lien n'est pas sur la partie prenante elle-même
			if (pp.getConjointPartiePrenante() == pp) {
				errorCollector.addNewMessage("Partie prenante mariée/pacsée avec elle-même !");
			}
		}
	}

	private static void checkCommune(int noOfsCommune, RegDate dateActe, Commune commune, MessageCollector errorCollector, boolean checkCommunePrincipale) throws EvenementReqDesException {
		if (commune == null) {
			errorCollector.addNewMessage(String.format("Commune %d inconnue au %s.", noOfsCommune, RegDateHelper.dateToDisplayString(dateActe)));
		}
		else if (checkCommunePrincipale && commune.isPrincipale()) {
			errorCollector.addNewMessage(String.format("La commune '%s' (%d) est une commune faîtière de fractions.", commune.getNomOfficiel(), noOfsCommune));
		}
	}

	private static final class ProcessingDataPartiePrenante {
		public final PersonnePhysique personnePhysique;
		public final List<String> elementsRemarque = new LinkedList<>();
		public final boolean creation;

		private ProcessingDataPartiePrenante(boolean creation, PersonnePhysique personnePhysique) {
			this.creation = creation;
			this.personnePhysique = personnePhysique;
		}
	}

	/**
	 * Travail de mise à jour du modèle de données des contribuables par rapports aux données reçues
	 * @param evt événement reçu de ReqDes
	 * @param partiesPrenantes liste des parties prenantes de l'unité de traitement
	 * @param warningCollector collecteurs d'avertissements
	 * @throws EvenementReqDesException en cas d'erreur
	 */
	private void doProcessing(EvenementReqDes evt, Set<PartiePrenante> partiesPrenantes, MessageCollector warningCollector) throws EvenementReqDesException {

		// la clé est l'ID de la partie prenante...
		final Map<Long, ProcessingDataPartiePrenante> processingData = findOrCreatePersonnesPhysiques(evt, partiesPrenantes, warningCollector);

		// gestion des états civils : on lie éventuellement les contribuables ensemble
		final List<ProcessingDataPartiePrenante> nouvellesCreations = gererEtatsCivils(evt, partiesPrenantes, processingData);

		// mise à jour des fors fiscaux
		gererForsFiscaux(evt, partiesPrenantes, processingData);

		// finalement, on ajoute les remarques sur les parties prenantes créées ou modifiées
		for (ProcessingDataPartiePrenante data : nouvellesCreations) {
			addRemarque(data, evt);
		}
		for (ProcessingDataPartiePrenante data : processingData.values()) {
			addRemarque(data, evt);
		}
	}

	private void addRemarque(ProcessingDataPartiePrenante data, EvenementReqDes evt) {
		if (data != null && data.personnePhysique != null) {
			if (data.creation) {
				addRemarqueCreation(data.personnePhysique, evt);
			}
			else if (!data.elementsRemarque.isEmpty()) {
				addRemarqueModification(data.personnePhysique, data.elementsRemarque, evt);
			}
		}
	}

	/**
	 * Création / modification des fors fiscaux
	 * @param evt événement reçu de ReqDes
	 * @param partiesPrenantes liste des parties prenantes de l'unité de traitement
	 * @param processingData données des personnes physiques indexées par identifiant de partie prenante
	 * @throws EvenementReqDesException en cas d'erreur
	 */
	private void gererForsFiscaux(EvenementReqDes evt, Set<PartiePrenante> partiesPrenantes, Map<Long, ProcessingDataPartiePrenante> processingData) throws EvenementReqDesException {
		final RegDate dateActe = evt.getDateActe();

		// parcours de toutes les parties prenantes qui ont un contribuable associé
		for (PartiePrenante partiePrenante : partiesPrenantes) {

			// on ne s'intéresse qu'aux parties prenantes qui ont un pendant fiscal
			final ProcessingDataPartiePrenante data = processingData.get(partiePrenante.getId());
			if (data != null) {

				// sur qui faut-il mettre un éventuel for ?
				final ContribuableImpositionPersonnesPhysiques assujetti;
				final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(data.personnePhysique, dateActe);
				if (couple != null) {
					assujetti = couple.getMenage();
				}
				else {
					assujetti = data.personnePhysique;
				}
				final ForsParTypeAt forsAt = assujetti.getForsParTypeAt(dateActe, false);
				final boolean hasRoleAcquereurPropriete = hasRoleAcquereurPropriete(partiePrenante);

				// [SIFISC-13333] si la partie prenante est indiquée comme décédée, on doit gérer le décès à la date de décès annoncée (sauf si on le sait déjà...)
				if (partiePrenante.getDateDeces() != null) {

					// un décédé qui achète ? il vaut mieux confier cela aux mains expertes de la cellule...
					if (hasRoleAcquereurPropriete) {
						throw new EvenementReqDesException("Le traitement automatique de l'acquisition de propriété par une partie prenante décédée n'est pas implémenté.");
					}

					// s'il y a un for principal ouvert (= on n'a pas encore traité le décès de la personne, donc "on ne le sait pas encore"), il faudra le fermer pour motif DECES
					if (forsAt.principal != null) {
						try {
							metierService.deces(data.personnePhysique, partiePrenante.getDateDeces(), null, null);
						}
						catch (MetierServiceException e) {
							throw new EvenementReqDesException(String.format("Impossible de traiter le décès de la partie prenante (contribuable %s) indiquée comme décédée au %s.",
							                                                 FormatNumeroHelper.numeroCTBToDisplay(data.personnePhysique.getNumero()),
							                                                 RegDateHelper.dateToDisplayString(partiePrenante.getDateDeces())), e);
						}
					}
				}

				// s'il y a un rôle acquéreur de propriété, on doit placer le for principal correctement
				// (même dans les autres cas de rôles, il peut être nécessaire d'ajuster le for principal)
				else if (hasRoleAcquereurPropriete || forsAt.principal != null) {

					//
					// d'abord on se préoccupe du for principal
					// 1. y en a-t-il déjà un ?
					// 1.1. si non, on le crée, c'est fini
					// 1.2. si oui, est-il fondamentalement différent de celui que l'on veut créer ?
					// 1.2.1. si non, one ne change rien
					// 1.2.2. si oui, on ferme/annule le for précédent pour créer le nouveau
					//

					final ForFiscalPrincipalPP ffpExistant = (ForFiscalPrincipalPP) forsAt.principal;
					final ModeImposition modeImposition;
					if (hasRoleAcquereurPropriete || !forsAt.secondaires.isEmpty()) {
						if (ffpExistant != null && ffpExistant.getModeImposition() == ModeImposition.SOURCE) {
							modeImposition = ffpExistant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD ? ModeImposition.MIXTE_137_1 : ModeImposition.ORDINAIRE;
						}
						else if (ffpExistant != null) {
							modeImposition = ffpExistant.getModeImposition();
						}
						else {
							modeImposition = ModeImposition.ORDINAIRE;
						}
					}
					else {
						modeImposition = ffpExistant.getModeImposition();
					}

					// si le for principal actif au moment de la date de l'acte est déjà fermé, on part en erreur -> à résoudre manuellement
					if (ffpExistant != null && ffpExistant.getDateFin() != null) {
						throw new EvenementReqDesException(String.format("Le for principal actif du tiers %s à la date de l'acte est déjà fermé. Cas à traiter manuellement.", FormatNumeroHelper.numeroCTBToDisplay(assujetti.getNumero())));
					}

					final Pair<Integer, TypeAutoriteFiscale> newLocalisation = computeNewLocalisation(partiePrenante.getOfsCommune(), partiePrenante.getOfsPays());
					if (ffpExistant != null) {
						final MotifFor motifOuverture;
						if (ffpExistant.getTypeAutoriteFiscale() != newLocalisation.getRight() || !ffpExistant.getNumeroOfsAutoriteFiscale().equals(newLocalisation.getLeft())) {
							if (ffpExistant.getTypeAutoriteFiscale() != newLocalisation.getRight()) {
								// changement de HS->HC ou HC->HS
								motifOuverture = ffpExistant.getTypeAutoriteFiscale() == TypeAutoriteFiscale.PAYS_HS ? MotifFor.ARRIVEE_HS : MotifFor.DEPART_HS;
							}
							else {
								// on reste dans le même type d'autorité fiscale -> HC->HC ou HS->HS
								motifOuverture = MotifFor.DEMENAGEMENT_VD;
							}
						}
						else if (modeImposition != ffpExistant.getModeImposition()) {
							motifOuverture = MotifFor.CHGT_MODE_IMPOSITION;
						}
						else {
							motifOuverture = null;
						}

						if (motifOuverture != null) {
							tiersService.closeForFiscalPrincipal(ffpExistant, dateActe.getOneDayBefore(), motifOuverture);
							tiersService.openForFiscalPrincipal(assujetti, dateActe, ffpExistant.getMotifRattachement(), newLocalisation.getLeft(), newLocalisation.getRight(), modeImposition, motifOuverture);
						}
					}
					else {
						// [SIFISC-15290] A la création du tout premier for principal HC/HS par ReqDes, le motif d'ouverture doit être laissé vide
						tiersService.openForFiscalPrincipal(assujetti, dateActe, MotifRattachement.DOMICILE, newLocalisation.getLeft(), newLocalisation.getRight(), modeImposition, null);
					}
				}

				// les fors secondaires ne sont créés que sur les acquéreurs de propriété
				if (hasRoleAcquereurPropriete) {

					// fors fiscaux existants
					final List<ForFiscalSecondaire> forsSecondaires = forsAt.secondaires;

					// on récupère la liste des numéros OFS des communes des fors secondaires, pour ne pas créer de doublon
					final Set<RolePartiePrenante> roles = partiePrenante.getRoles();
					final Set<Pair<Integer, MotifRattachement>> ofsCommunesExistantes = new HashSet<>(forsSecondaires.size() + roles.size());
					for (ForFiscalSecondaire ffs : forsSecondaires) {
						ofsCommunesExistantes.add(Pair.of(ffs.getNumeroOfsAutoriteFiscale(), ffs.getMotifRattachement()));
					}

					// parcours de tous les rôles de la partie prenante
					for (RolePartiePrenante role : roles) {
						// on ne s'intéresse qu'aux acquisitions de propriété
						if (isRoleAcquereurPropriete(role)) {
							final Integer ofsCommune = role.getTransaction().getOfsCommune();
							final Pair<Integer, MotifRattachement> key = Pair.of(ofsCommune, MotifRattachement.IMMEUBLE_PRIVE);
							if (!ofsCommunesExistantes.contains(key)) {
								// et finalement on crée les fors secondaires sur les communes où il n'y en a pas encore
								tiersService.openForFiscalSecondaire(assujetti, dateActe, MotifRattachement.IMMEUBLE_PRIVE, ofsCommune, TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD, MotifFor.ACHAT_IMMOBILIER, GenreImpot.REVENU_FORTUNE);
								ofsCommunesExistantes.add(key);
							}
						}
					}
				}
			}
		}
	}

	private static Pair<Integer, TypeAutoriteFiscale> computeNewLocalisation(@Nullable Integer ofsCommune, @Nullable Integer ofsPays) {
		final int noOfs;
		final TypeAutoriteFiscale typeAutoriteFiscale;
		if (ofsCommune != null) {
			noOfs = ofsCommune;
			typeAutoriteFiscale = TypeAutoriteFiscale.COMMUNE_HC;
		}
		else if (ofsPays != null) {
			noOfs = ofsPays;
			typeAutoriteFiscale = TypeAutoriteFiscale.PAYS_HS;
		}
		else {
			noOfs = ServiceInfrastructureService.noPaysInconnu;
			typeAutoriteFiscale = TypeAutoriteFiscale.PAYS_HS;
		}
		return Pair.of(noOfs, typeAutoriteFiscale);
	}

	private static boolean hasRoleAcquereurPropriete(PartiePrenante partiePrenante) {
		// parcours de tous les rôles de la partie prenante
		for (RolePartiePrenante role : partiePrenante.getRoles()) {
			if (isRoleAcquereurPropriete(role)) {
				return true;
			}
		}
		return false;
	}

	private static boolean isRoleAcquereurPropriete(RolePartiePrenante role) {
		return role.getRole() == TypeRole.ACQUEREUR && role.getTransaction().getTypeInscription() == TypeInscription.PROPRIETE;
	}

	/**
	 * Gestion des états civils (= création/modification de couples...)
	 * @param evt événement reçu de ReqDes
	 * @param partiesPrenantes liste des parties prenantes de l'unité de traitement
	 * @param processingData données des personnes physiques indexées par identifiant de partie prenante
	 * @return les données des nouvelles personnes physiques créées exprès ici pour constituer des couples
	 * @throws EvenementReqDesException en cas d'erreur
	 */
	private List<ProcessingDataPartiePrenante> gererEtatsCivils(EvenementReqDes evt, Set<PartiePrenante> partiesPrenantes, Map<Long, ProcessingDataPartiePrenante> processingData) throws EvenementReqDesException {
		final RegDate dateActe = evt.getDateActe();
		final List<ProcessingDataPartiePrenante> nouvellesCreations = new LinkedList<>();
		for (PartiePrenante ppSrc : partiesPrenantes) {
			final ProcessingDataPartiePrenante data = processingData.get(ppSrc.getId());
			if (data != null) {
				final EtatCivil etatCivil = ppSrc.getEtatCivil();
				final boolean conjointAutrePartiePrenante = ppSrc.getConjointPartiePrenante() != null;

				// cela concerne-t-il un couple ? (Attention : les états civils "séparés" sont fournis comme "mariés" avec une date de séparation renseignée)
				if (etatCivil == EtatCivil.LIE_PARTENARIAT_ENREGISTRE || etatCivil == EtatCivil.MARIE) {
					// qui est le conjoint ?
					final ProcessingDataPartiePrenante conjointData;
					if (conjointAutrePartiePrenante) {
						if (ppSrc.getConjointPartiePrenante().isSourceCivile()) {
							// on crée un conjoint bidon (a priori un doublon, mais bon...)
							final PersonnePhysique conjoint = createPersonnePhysique(ppSrc.getConjointPartiePrenante());
							conjointData = new ProcessingDataPartiePrenante(true, conjoint);
							nouvellesCreations.add(conjointData);
						}
						else {
							// c'est l'autre partie prenante, également fiscale
							conjointData = processingData.get(ppSrc.getConjointPartiePrenante().getId());
						}
					}
					else if (StringUtils.isNotBlank(ppSrc.getNomConjoint())) {
						final PersonnePhysique conjoint = createConjointMinimal(ppSrc.getNomConjoint(), ppSrc.getPrenomConjoint());
						conjointData = new ProcessingDataPartiePrenante(true, conjoint);
						nouvellesCreations.add(conjointData);
					}
					else {
						conjointData = null;
					}

					final ModeTraitementCouple modeTraitementCouple = determineModeTraitementCouple(data.creation, conjointData != null ? conjointData.creation : null);
					if (modeTraitementCouple == ModeTraitementCouple.CREATION_PURE) {
						// on ne s'embête pas, on crée le couple
						final PersonnePhysique conjoint = conjointData != null ? conjointData.personnePhysique : null;
						final EnsembleTiersCouple couple = tiersService.createEnsembleTiersCouple(data.personnePhysique, conjoint, ppSrc.getDateEtatCivil(), ppSrc.getDateSeparation() != null ? ppSrc.getDateSeparation().getOneDayBefore() : null);
						final MenageCommun mc = couple.getMenage();
						addRemarqueCreation(mc, evt);
					}
					else if (modeTraitementCouple == ModeTraitementCouple.MIXTE) {
						// ici, on a forcément un couple complet : l'un est nouveau, l'autre déjà connu
						final PersonnePhysique connu = data.creation ? conjointData.personnePhysique : data.personnePhysique;
						final PersonnePhysique nouveau = data.creation ? data.personnePhysique : conjointData.personnePhysique;

						// que connait-on pout le moment ?
						final EnsembleTiersCouple coupleConnu = tiersService.getEnsembleTiersCouple(connu, ppSrc.getDateEtatCivil());
						if (coupleConnu != null) {
							final PersonnePhysique conjointConnu = coupleConnu.getConjoint(connu);
							if (conjointConnu == null) {
								// connu comme marié seul -> on complète ?
								tiersService.addTiersToCouple(coupleConnu.getMenage(), nouveau, ppSrc.getDateEtatCivil(), ppSrc.getDateSeparation() != null ? ppSrc.getDateSeparation().getOneDayBefore() : null);
							}
							else if (isMemeNom(nouveau, conjointConnu)) {
								// il faut détruire le nouveau (et en cascade tout ce qu'on a pu faire sur lui), qui ne sert plus à rien (et du coup on ne met pas à jour le conjoint connu !!)
								hibernateTemplate.delete(nouveau);

								// on l'enlève également des nouveautés (il ne faudrait pas tenter de lui rajouter une remarque alors qu'il est déjà mort...) s'il y est
								if (!data.creation) {
									nouvellesCreations.remove(conjointData);
								}

								// et on l'enlève du mapping des parties prenantes s'il y est
								processingData.values().remove(data.creation ? data : conjointData);
							}
							else {
								throw new EvenementReqDesException(String.format("Le conjoint du tiers %s (%s) n'a pas le même nom que celui qui est annoncé dans l'acte.",
								                                                 FormatNumeroHelper.numeroCTBToDisplay(connu.getNumero()),
								                                                 FormatNumeroHelper.numeroCTBToDisplay(conjointConnu.getNumero())));
							}
						}
						else {
							throw new EvenementReqDesException(String.format("Le tiers %s n'est pas connu en couple au %s.",
							                                                 FormatNumeroHelper.numeroCTBToDisplay(data.personnePhysique.getNumero()),
							                                                 RegDateHelper.dateToDisplayString(ppSrc.getDateEtatCivil())));
						}
					}
					else if (modeTraitementCouple == ModeTraitementCouple.MODIFICATION_PURE) {

						final boolean avecSeparation = ppSrc.getDateSeparation() != null;

						// ou bien les deux sont en modification, ou bien on a affaire à un marié seul
						if (conjointData != null) {

							// pour les tests, c'est mieux d'avoir les deux numéros de contribuables dans un ordre déterministe... et cela ne gêne absolument pas pour la production -> on le fait
							final List<Long> ids = Arrays.asList(data.personnePhysique.getNumero(), conjointData.personnePhysique.getNumero());
							Collections.sort(ids);

							// s'il y a déclaration de séparation dans l'acte et que le couple n'est pas séparé dans le fiscal à la date déclarée de séparation, on arrête
							if (avecSeparation) {
								final EnsembleTiersCouple couplePrincipal = tiersService.getEnsembleTiersCouple(data.personnePhysique, ppSrc.getDateSeparation());
								final EnsembleTiersCouple coupleConjoint = tiersService.getEnsembleTiersCouple(conjointData.personnePhysique, ppSrc.getDateSeparation());
								if (couplePrincipal != null || coupleConjoint != null) {
									throw new EvenementReqDesException(String.format("Les tiers %s et %s ne sont pas séparés fiscalement au %s.",
									                                                 FormatNumeroHelper.numeroCTBToDisplay(ids.get(0)),
									                                                 FormatNumeroHelper.numeroCTBToDisplay(ids.get(1)),
									                                                 RegDateHelper.dateToDisplayString(ppSrc.getDateSeparation())));
								}
							}
							else {
								// pas de séparation -> ils sont donc toujours en couple, normalement
								final EnsembleTiersCouple couplePrincipal = tiersService.getEnsembleTiersCouple(data.personnePhysique, dateActe);
								final EnsembleTiersCouple coupleConjoint = tiersService.getEnsembleTiersCouple(conjointData.personnePhysique, dateActe);
								if (couplePrincipal == null || coupleConjoint == null || couplePrincipal.getMenage() != coupleConjoint.getMenage()) {
									throw new EvenementReqDesException(String.format("Les tiers %s et %s ne forment pas un couple au %s.",
									                                                 FormatNumeroHelper.numeroCTBToDisplay(ids.get(0)),
									                                                 FormatNumeroHelper.numeroCTBToDisplay(ids.get(1)),
									                                                 RegDateHelper.dateToDisplayString(dateActe)));
								}
							}

							// les deux sont en modification... il faut juste vérifier que c'est le bon couple et c'est bon
							final EnsembleTiersCouple couplePrincipal = tiersService.getEnsembleTiersCouple(data.personnePhysique, ppSrc.getDateEtatCivil());
							final EnsembleTiersCouple coupleConjoint = tiersService.getEnsembleTiersCouple(conjointData.personnePhysique, ppSrc.getDateEtatCivil());
							if (couplePrincipal == null || coupleConjoint == null || couplePrincipal.getMenage() != coupleConjoint.getMenage()) {
								throw new EvenementReqDesException(String.format("Les tiers %s et %s ne forment pas un couple au %s.",
								                                                 FormatNumeroHelper.numeroCTBToDisplay(ids.get(0)),
								                                                 FormatNumeroHelper.numeroCTBToDisplay(ids.get(1)),
								                                                 RegDateHelper.dateToDisplayString(ppSrc.getDateEtatCivil())));
							}
						}
						else {

							// s'il y a déclaration de séparation dans l'acte, il ne doit pas y avoir de couple actif fiscalement à la date de séparation déclarée
							if (avecSeparation) {
								final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(data.personnePhysique, ppSrc.getDateSeparation());
								if (couple != null) {
									throw new EvenementReqDesException(String.format("Le tiers %s est en couple au %s.",
									                                                 FormatNumeroHelper.numeroCTBToDisplay(data.personnePhysique.getNumero()),
									                                                 RegDateHelper.dateToDisplayString(ppSrc.getDateSeparation())));
								}
							}
							else {
								// pas de séparation, le couple doit donc exister au moment de l'acte
								final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(data.personnePhysique, dateActe);
								if (couple == null) {
									throw new EvenementReqDesException(String.format("Le tiers %s n'est pas en couple au %s.",
									                                                 FormatNumeroHelper.numeroCTBToDisplay(data.personnePhysique.getNumero()),
									                                                 RegDateHelper.dateToDisplayString(dateActe)));
								}
							}

							// marié seul -> si le tiers connu est en couple en base, c'est tout bon
							final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(data.personnePhysique, ppSrc.getDateEtatCivil());
							if (couple == null) {
								throw new EvenementReqDesException(String.format("Le tiers %s n'est pas en couple au %s.",
								                                                 FormatNumeroHelper.numeroCTBToDisplay(data.personnePhysique.getNumero()),
								                                                 RegDateHelper.dateToDisplayString(ppSrc.getDateEtatCivil())));
							}
						}
					}
					else {
						// ceci est un bug!
						throw new IllegalArgumentException("Cas non traité de mode de traitement de couple : " + modeTraitementCouple);
					}

					// c'est la dernière fois que l'on voit ce conjoint, il faut dont le mettre à jour complètement dès maintenant
					if (conjointData != null) {
						final PartiePrenante conjointPartiePrenante = conjointAutrePartiePrenante ? ppSrc.getConjointPartiePrenante() : ppSrc;
						gererSituationFamille(conjointPartiePrenante.getDateEtatCivil(), conjointPartiePrenante.getEtatCivil(), conjointPartiePrenante.getDateSeparation(), conjointData);
					}
				}

				// "etatCivil == null" signifie "partie prenante décédée"
				else if (!data.creation && etatCivil != null) {
					// la partie prenante est indiquée comme "équivalent célibataire" et il s'agit d'une mise à jour -> vérifions qu'on a bien quelque chose de compatible en base
					final Set<RapportEntreTiers> rapports = data.personnePhysique.getRapportsSujet();
					final DateRange range = new DateRangeHelper.Range(ppSrc.getDateEtatCivil(), null);
					for (RapportEntreTiers ret : rapports) {
						if (!ret.isAnnule() && ret instanceof AppartenanceMenage && DateRangeHelper.intersect(range, ret)) {
							// visiblement, on connait cette personne en couple après la date de l'acte...
							throw new EvenementReqDesException(String.format("La personne physique %s est connue en couple après le %s mais est indiquée comme %s dans l'acte.",
							                                                 FormatNumeroHelper.numeroCTBToDisplay(data.personnePhysique.getNumero()),
							                                                 RegDateHelper.dateToDisplayString(ppSrc.getDateEtatCivil()),
							                                                 etatCivil.format()));
						}
					}
				}

				// couple ou pas couple, on crée (resp. met à jour) la situation de famille fiscale sur la personne physique
				gererSituationFamille(ppSrc.getDateEtatCivil(), etatCivil, ppSrc.getDateSeparation(), data);

				// pas la peine d'aller regarder l'autre partie prenante si on a déjà constitué un couple avec elle...
				if (conjointAutrePartiePrenante) {
					break;
				}
			}
		}
		return nouvellesCreations;
	}

	private boolean isMemeNom(PersonnePhysique nouvellementCree, PersonnePhysique dejaDansUnireg) {
		final NomPrenom nomPrenomExistant = tiersService.getDecompositionNomPrenom(dejaDansUnireg, true);
		final NomPrenom nomPrenomNouveau = tiersService.getDecompositionNomPrenom(nouvellementCree, true);
		return nomPrenomExistant.equals(nomPrenomNouveau);
	}

	private void gererSituationFamille(RegDate dateEtatCivil, EtatCivil etatCivil, @Nullable RegDate dateSeparation, ProcessingDataPartiePrenante data) {
		if (etatCivil != null && dateEtatCivil != null) {
			final List<SituationFamille> sfs = data.personnePhysique.getSituationsFamilleSorted();
			final RegDate effDateEtatCivil = getDateEffectiveEtatCivil(dateEtatCivil, dateSeparation);
			final SituationFamille sf = sfs != null ? DateRangeHelper.rangeAt(sfs, effDateEtatCivil) : null;
			final EtatCivil effEtatCivil = getEtatCivilEffectif(effDateEtatCivil, etatCivil, dateSeparation);
			if (sf == null || sf.getEtatCivil() != effEtatCivil) {
				// fermeture de la situation de famille encore ouverte et annulation des situations de familles éventuelles ultérieures
				if (sfs != null) {
					for (SituationFamille curseur : sfs) {
						if (RegDateHelper.isAfterOrEqual(curseur.getDateDebut(), effDateEtatCivil, NullDateBehavior.EARLIEST)) {
							curseur.setAnnule(true);
						}
						else if (curseur.getDateFin() == null) {
							curseur.setDateFin(effDateEtatCivil.getOneDayBefore());
						}
					}
				}

				// mise en place de la nouvelle situation de famille
				final SituationFamille newSf = new SituationFamillePersonnePhysique();
				newSf.setDateDebut(effDateEtatCivil);
				newSf.setEtatCivil(effEtatCivil);
				data.personnePhysique.addSituationFamille(newSf);

				if (!data.creation) {
					data.elementsRemarque.add(String.format("Etat civil au %s : %s -> %s",
					                                        RegDateHelper.dateToDisplayString(effDateEtatCivil),
					                                        ETAT_CIVIL_RENDERER.toString(sf != null ? sf.getEtatCivil() : null),
					                                        ETAT_CIVIL_RENDERER.toString(effEtatCivil)));
				}
			}
		}
	}

	private static RegDate getDateEffectiveEtatCivil(RegDate dateEtatCivil, @Nullable RegDate dateSeparation) {
		return RegDateHelper.maximum(dateEtatCivil, dateSeparation, NullDateBehavior.EARLIEST);
	}

	private static EtatCivil getEtatCivilEffectif(RegDate dateReference, EtatCivil etatCivilPartiePrenante, @Nullable RegDate dateSeparation) {
		if (dateSeparation != null && dateSeparation.compareTo(dateReference) <= 0) {
			switch (etatCivilPartiePrenante) {
			case MARIE:
				return EtatCivil.SEPARE;
			case LIE_PARTENARIAT_ENREGISTRE:
				return EtatCivil.PARTENARIAT_SEPARE;
			}

			// TODO faudrait-il exploser ici, car on a une date de séparation avec un état civil qui ne le supporte pas... ?
		}
		return etatCivilPartiePrenante;
	}

	/**
	 * Crée (ou trouve et modifie les données simples + adresse) les personnes physiques associées aux parties prenantes
	 * @param evt événement reçu de ReqDes
	 * @param partiesPrenantes parties prenantes de l'unité de traitement
	 * @param warningCollector collecteurs d'avertissements
	 * @return map (la clé est l'ID de la partie prenante) des personnes physiques créées/trouvées (rien n'est créé si la source de la partie prenante est civile)
	 * @throws EvenementReqDesException en cas d'erreur
	 */
	private Map<Long, ProcessingDataPartiePrenante> findOrCreatePersonnesPhysiques(EvenementReqDes evt, Set<PartiePrenante> partiesPrenantes, MessageCollector warningCollector) throws EvenementReqDesException {
		final RegDate dateActe = evt.getDateActe();
		final Map<Long, ProcessingDataPartiePrenante> map = new HashMap<>(partiesPrenantes.size());
		for (PartiePrenante ppSrc : partiesPrenantes) {
			final ProcessingDataPartiePrenante data;
			if (ppSrc.isSourceCivile()) {
				// on ne fait rien sur une partie prenante qui vient du civil
				data = null;
			}
			else if (ppSrc.getNumeroContribuable() != null) {
				final Tiers tiers = tiersService.getTiers(ppSrc.getNumeroContribuable());
				if (tiers == null) {
					throw new EvenementReqDesException(String.format("Le numéro de contribuable %s ne correspond à rien de connu.", FormatNumeroHelper.numeroCTBToDisplay(ppSrc.getNumeroContribuable())));
				}
				if (!(tiers instanceof PersonnePhysique)) {
					throw new EvenementReqDesException(String.format("Le tiers %s n'est pas une personne physique.", FormatNumeroHelper.numeroCTBToDisplay(ppSrc.getNumeroContribuable())));
				}

				// [SIFISC-13300] si le contribuable est annulé, i107 ou désactivé à la date du jour de traitement, on refuse la mise à jour automatique
				if (tiers.isDesactive(null) || tiers.isDebiteurInactif()) {
					throw new EvenementReqDesException(String.format("Le tiers %s est inactif.", FormatNumeroHelper.numeroCTBToDisplay(ppSrc.getNumeroContribuable())));
				}

				final PersonnePhysique ppDest = (PersonnePhysique) tiers;
				data = new ProcessingDataPartiePrenante(false, ppDest);

				// vérification que l'on n'est pas en train de vouloir modifier un Vaudois
				checkForEtResidenceAvantModification(ppDest, dateActe);

				// mise à jour des informations de base et premiers éléments constitutifs de la remarque éventuelle à ajouter au tiers
				data.elementsRemarque.addAll(updatePersonnePhysiqueConnue(ppSrc, ppDest));

				// mise à jour de l'adresse courrier (ou addition d'une nouvelle remarque)
				data.elementsRemarque.addAll(updateAdresse(ppSrc, ppDest, dateActe, warningCollector));
			}
			else {
				final PersonnePhysique ppDest = createPersonnePhysique(ppSrc);
				addAdresseCourrier(ppSrc, ppDest, dateActe);

				// [SIFISC-13397] on conserve dans la partie prenante le lien vers la personne physique créée
				ppSrc.setNumeroContribuableCree(ppDest.getNumero());

				data = new ProcessingDataPartiePrenante(true, ppDest);
			}

			// on collecte les personnes physiques concernées
			map.put(ppSrc.getId(), data);
		}
		return map;
	}

	private enum ModeTraitementCouple {
		/**
		 * les deux conjoints connus viennent d'être créés
		 */
		CREATION_PURE,

		/**
		 * les deux conjoints connus existaient déjà
		 */
		MODIFICATION_PURE,

		/**
		 * l'un des conjoints a été créé, l'autre existait déjà
		 */
		MIXTE
	}

	/**
	 * @param modeCreationPrincipal <code>true</code> si le contribuable principal a été créé par ce traitement, <code>false</code> s'il existait déjà
	 * @param modeCreationConjoint <code>true</code> si le contribuable principal a été créé par ce traitement, <code>false</code> s'il existait déjà, et <code>null</code> s'il est inconnu
	 * @return le mode de création du couple
	 */
	private static ModeTraitementCouple determineModeTraitementCouple(boolean modeCreationPrincipal, @Nullable Boolean modeCreationConjoint) {
		if (modeCreationConjoint == null || modeCreationPrincipal == modeCreationConjoint) {
			return modeCreationPrincipal ? ModeTraitementCouple.CREATION_PURE : ModeTraitementCouple.MODIFICATION_PURE;
		}
		else {
			return ModeTraitementCouple.MIXTE;
		}
	}

	/**
	 * @param pp partie prenante source des données
	 * @param dateDebut date de début de validité de la nouvelle adresse
	 * @return une adresse construite mais pas encore associée à un tiers ni enregistrée dans la session hibernate
	 * @throws EvenementReqDesException en cas de problème
	 */
	private AdresseSupplementaire buildAdresseCourrier(PartiePrenante pp, RegDate dateDebut) throws EvenementReqDesException {
		if (pp.getOfsPays() == ServiceInfrastructureService.noOfsSuisse) {
			final AdresseSuisse a = new AdresseSuisse();
			a.setComplement(pp.getTitre());
			a.setDateDebut(dateDebut);
			a.setNumeroAppartement(pp.getNumeroAppartement());
			a.setNumeroCasePostale(pp.getCasePostale());
			a.setNumeroMaison(pp.getNumeroMaison());
			a.setNumeroOrdrePoste(pp.getNumeroOrdrePostal() != null ? pp.getNumeroOrdrePostal() : findNumeroOrdrePostal(pp.getId(), pp.getLocalite(), pp.getNumeroPostal(), pp.getNumeroPostalComplementaire(), dateDebut));
			a.setRue(pp.getRue());
			a.setTexteCasePostale(StringUtils.isNotBlank(pp.getTexteCasePostale()) || pp.getCasePostale() != null ? TexteCasePostale.parse(pp.getTexteCasePostale()) : null);
			a.setUsage(TypeAdresseTiers.COURRIER);
			return a;
		}
		else {
			final AdresseEtrangere a = new AdresseEtrangere();
			a.setComplement(pp.getTitre());
			a.setDateDebut(dateDebut);
			a.setNumeroAppartement(pp.getNumeroAppartement());
			a.setNumeroCasePostale(pp.getCasePostale());
			a.setNumeroMaison(pp.getNumeroMaison());
			a.setNumeroOfsPays(pp.getOfsPays());
			a.setNumeroPostalLocalite(StringUtils.trimToNull(StringUtils.trimToEmpty(pp.getNumeroPostal()) + " " + StringUtils.trimToEmpty(pp.getLocalite())));
			a.setRue(pp.getRue());
			a.setTexteCasePostale(StringUtils.isNotBlank(pp.getTexteCasePostale()) || pp.getCasePostale() != null ? TexteCasePostale.parse(pp.getTexteCasePostale()) : null);
			a.setUsage(TypeAdresseTiers.COURRIER);
			return a;
		}
	}

	private Integer findNumeroOrdrePostal(long idPartiePrenante, String localite, String npaString, Integer npaComplement, RegDate dateReference) throws EvenementReqDesException {
		final List<Localite> localites = infraService.getLocalites();
		final Integer npa;
		if (npaString != null && Pattern.matches("\\d+", npaString)) {
			npa = Integer.valueOf(npaString);
		}
		else {
			npa = null;
		}

		final List<Localite> candidates = new LinkedList<>();
		for (Localite candidate : localites) {
			if (candidate.isValidAt(dateReference)
					&& (npa == null || npa.equals(candidate.getNPA()))
					&& (candidate.getNom().equalsIgnoreCase(localite) || candidate.getNomAbrege().equalsIgnoreCase(localite))
					&& (npaComplement == null || npaComplement.equals(candidate.getComplementNPA()))) {
				candidates.add(candidate);
			}
		}

		if (candidates.isEmpty()) {
			throw new EvenementReqDesException("Localité inconnue dans l'adresse suisse fournie.");
		}
		else if (candidates.size() == 1) {
			// on a trouvé LA localité
			return candidates.get(0).getNoOrdre();
		}
		else {
			final StringBuilder b = new StringBuilder();
			b.append("Plusieurs ONRP potentiels pour l'adresse suisse fournie dans la partie prenante ").append(idPartiePrenante).append(" : ");
			for (Localite l : candidates) {
				b.append(l.getNom()).append(" (").append(l.getNoOrdre()).append(") ");
			}
			LOGGER.warn(b.toString());

			throw new EvenementReqDesException("Informations insuffisantes pour déterminer la localité exacte de l'adresse suisse fournie.");
		}
	}

	private void addAdresseCourrier(PartiePrenante ppSrc, PersonnePhysique ppDest, RegDate dateDebut) throws EvenementReqDesException {
		final AdresseTiers adresse = buildAdresseCourrier(ppSrc, dateDebut);
		ppDest.addAdresseTiers(adresse);
	}

	/**
	 * Construit une ligne de remarque avec la destination de l'adresse sur 4 lignes max si de telles lignes existent
	 * @param adresse adresse source
	 * @return une liste (potentiellement vide) des parties de remarque à ajouter
	 */
	private List<String> buildRemarqueAdresseTransmiseNonEnregistree(PersonnePhysique pp, AdresseGenerique adresse, RegDate date, MessageCollector warningCollector) throws EvenementReqDesException {

		try {
			final AdresseEnvoi envoi = adresseService.buildAdresseEnvoi(pp, adresse, date);

			// récupération des lignes d'adresse non-vides
			final String[] lignesBruttes = envoi.getLignes();
			final List<String> lignes = new ArrayList<>(lignesBruttes.length);
			for (String ligne : lignesBruttes) {
				if (StringUtils.isNotBlank(ligne)) {
					lignes.add(ligne);
				}
			}

			// sur une personne physique, les deux premières lignes sont les salutations et le nom, que l'on ne reproduit pas ici
			if (lignes.size() > 2) {
				final StringBuilder b = new StringBuilder();
				b.append("Adresse transmise non enregistrée : ");

				b.append(lignes.get(2));
				for (int i = 3 ; i < lignes.size() ; ++ i) {
					b.append(" / ").append(lignes.get(i));
				}

				// pour signaler la non-prise en compte de l'adresse
				warningCollector.addNewMessage(String.format("Adresse non modifiée sur un contribuable %s, uniquement reprise dans les remarques du tiers.", pp.isDecede() ? "décédé" : "assujetti"));

				// ce texte sera inclu dans la remarque
				return Collections.singletonList(b.toString());
			}

			// pas d'adresse significative -> rien de spécial
			return Collections.emptyList();
		}
		catch (AdresseException e) {
			throw new EvenementReqDesException(e);
		}
	}

	private List<String> updateAdresse(PartiePrenante ppSrc, PersonnePhysique ppDest, RegDate dateDebut, MessageCollector warningCollector) throws EvenementReqDesException {
		// le contribuable (ou son éventuel ménage) est-il assujetti au rôle ordinaire à la date de référence ?
		// [SIFISC-15326] si le contribuable est décédé, on ne change pas l'adresse (pour ne pas effacer une adresse éventuellement déjà mise en place par les successions)
		if (isAssujettisRoleOrdinaire(ppDest, dateDebut) || ppDest.isDecede()) {
			final AdresseSupplementaire nvelle = buildAdresseCourrier(ppSrc, dateDebut);
			final AdresseSupplementaireAdapter adapter = new AdresseSupplementaireAdapter(nvelle, ppDest, false, infraService);
			return buildRemarqueAdresseTransmiseNonEnregistree(ppDest, adapter, dateDebut, warningCollector);
		}
		else {
			// on ferme l'adresse courrier éventuelle du contribuable à la veille (et on annule toute surcharge d'adresse courrier qui commencerait
			// après cette date)
			final List<AdresseTiers> existantes = ppDest.getAdressesTiersSorted(TypeAdresseTiers.COURRIER);
			for (AdresseTiers existante : existantes) {
				if (existante.isValidAt(dateDebut)) {
					// deux cas : l'adresse est toujours ouverte, auquel cas il suffit de la fermer ; ou l'adresse est déjà fermée, auquel cas il faut l'annuler
					// et en re-créer une autre avec les bonnes dates
					if (existante.getDateFin() == null) {
						existante.setDateFin(dateDebut.getOneDayBefore());
					}
					else if (existante.getDateFin() != dateDebut.getOneDayBefore()) {
						final AdresseTiers replacement = existante.duplicate();
						existante.setAnnule(true);
						replacement.setDateFin(dateDebut.getOneDayBefore());
						ppDest.addAdresseTiers(replacement);
					}
				}
				else if (!existante.isAnnule() && RegDateHelper.isAfterOrEqual(existante.getDateDebut(), dateDebut, NullDateBehavior.EARLIEST)) {
					existante.setAnnule(true);
				}
			}

			// et on ajoute la nouvelle
			ppDest.addAdresseTiers(buildAdresseCourrier(ppSrc, dateDebut));

			// rien dans les remarques
			return Collections.emptyList();
		}
	}

	private boolean isAssujettisRoleOrdinaire(PersonnePhysique pp, RegDate date) throws EvenementReqDesException {
		final Set<TypeAssujettissement> acceptes = EnumSet.of(TypeAssujettissement.DIPLOMATE_SUISSE,
		                                                      TypeAssujettissement.HORS_CANTON,
		                                                      TypeAssujettissement.HORS_SUISSE,
		                                                      TypeAssujettissement.INDIGENT,
		                                                      TypeAssujettissement.MIXTE_137_1,
		                                                      TypeAssujettissement.MIXTE_137_2,
		                                                      TypeAssujettissement.VAUDOIS_DEPENSE,
		                                                      TypeAssujettissement.VAUDOIS_ORDINAIRE);

		try {
			// d'abord sur la personne physique
			final List<Assujettissement> app = assujettissementService.determine(pp);
			if (app != null) {
				final Assujettissement a = DateRangeHelper.rangeAt(app, date);
				if (a != null && acceptes.contains(a.getType())) {
					return true;
				}
			}

			// puis sur un éventuel ménage commun valide à la date considérée
			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, date);
			if (couple != null) {
				final List<Assujettissement> amc = assujettissementService.determine(couple.getMenage());
				if (amc != null) {
					final Assujettissement a = DateRangeHelper.rangeAt(amc, date);
					if (a != null && acceptes.contains(a.getType())) {
						return true;
					}
				}
			}

			// et non, ou bien pas assujetti du tout, ou bien source pure
			return false;
		}
		catch (AssujettissementException e) {
			throw new EvenementReqDesException(e);
		}
	}

	private void checkForEtResidenceAvantModification(PersonnePhysique pp, RegDate dateActe) throws EvenementReqDesException {
		// vérification des fors principaux à la date de l'acte
		final ForFiscalPrincipal ffp;
		final Set<Integer> ofsCommunesResidence = new HashSet<>();
		final ForFiscalPrincipal ffpPersonnePhysique = pp.getForFiscalPrincipalAt(dateActe);
		if (ffpPersonnePhysique != null) {
			ffp = ffpPersonnePhysique;
			ofsCommunesResidence.addAll(getCommunesResidence(dateActe, pp));
		}
		else {
			// regardons un éventuel couple
			final EnsembleTiersCouple couple = tiersService.getEnsembleTiersCouple(pp, dateActe);
			if (couple != null && couple.getMenage() != null) {
				ffp = couple.getMenage().getForFiscalPrincipalAt(dateActe);
				ofsCommunesResidence.addAll(getCommunesResidence(dateActe, couple.getPrincipal(), couple.getConjoint()));
			}
			else {
				ffp = null;
			}
		}
		if (ffp != null && ffp.getTypeAutoriteFiscale() == TypeAutoriteFiscale.COMMUNE_OU_FRACTION_VD) {
			// for principal vaudois -> erreur
			throw new EvenementReqDesException("Mise-à-jour impossible pour contribuable(s) avec for principal vaudois à la date de l'acte.");
		}

		// vérification des communes de résidences
		for (int ofsCommune : ofsCommunesResidence) {
			try {
				final Commune commune = infraService.getCommuneByNumeroOfs(ofsCommune, dateActe);
				if (commune == null) {
					throw new EvenementReqDesException(String.format("Mise-à-jour impossible pour contribuable(s) avec commune de résidence inconnue (%d) à la date de l'acte.", ofsCommune));
				}
				if (commune.isVaudoise()) {
					throw new EvenementReqDesException(String.format("Mise-à-jour impossible pour contribuable(s) avec commune de résidence vaudoise (%s/%d) à la date de l'acte.", commune.getNomOfficiel(), ofsCommune));
				}
			}
			catch (ServiceInfrastructureException e) {
				throw new EvenementReqDesException("Erreur du service infrastructure", e);
			}
		}
	}

	private List<Integer> getCommunesResidence(RegDate dateActe, PersonnePhysique... pps) throws EvenementReqDesException {
		final List<Integer> communes = new ArrayList<>(pps.length);
		for (PersonnePhysique pp : pps) {
			try {
				final AdresseGenerique adresse = adresseService.getAdresseFiscale(pp, TypeAdresseFiscale.DOMICILE, dateActe, false);
				if (adresse != null && !adresse.isDefault() && adresse.getNoOfsCommuneAdresse() != null) {
					communes.add(adresse.getNoOfsCommuneAdresse());
				}
			}
			catch (AdresseException e) {
				throw new EvenementReqDesException(String.format("Impossible de trouver l'adresse de résidence du tiers %s", FormatNumeroHelper.numeroCTBToDisplay(pp.getNumero())), e);
			}
		}
		return communes;
	}

	/**
	 * @param ppSrc la partie prenante source des données
	 * @param ppDest la personne physique destination des données
	 * @return la liste (potentiellement vide) des descriptions des champs modifiés dans la personne physique
	 * @throws EvenementReqDesException en cas de problème
	 */
	private List<String> updatePersonnePhysiqueConnue(PartiePrenante ppSrc, PersonnePhysique ppDest) throws EvenementReqDesException {
		return dumpBaseDataToPersonnePhysique(ppSrc, ppDest, true);
	}

	private PersonnePhysique createPersonnePhysique(PartiePrenante ppSrc) throws EvenementReqDesException {
		final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
		dumpBaseDataToPersonnePhysique(ppSrc, pp, false);
		return hibernateTemplate.merge(pp);
	}

	private PersonnePhysique createConjointMinimal(String nom, String prenoms) {
		final PersonnePhysique pp = new PersonnePhysique(Boolean.FALSE);
		pp.setNom(nom);
		pp.setTousPrenoms(prenoms);
		pp.setPrenomUsuel(NomPrenom.extractPrenomUsuel(prenoms));
		return hibernateTemplate.merge(pp);
	}

	private List<String> dumpBaseDataToPersonnePhysique(PartiePrenante src, PersonnePhysique dest, boolean withRemarqueOnChange) {
		final List<String> elementsRemarque = new LinkedList<>();
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getNom(), NOM_ACCESSOR, withRemarqueOnChange, DEFAULT_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getNomNaissance(), NOM_NAISSANCE_ACCESSOR, withRemarqueOnChange, DEFAULT_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getPrenoms(), PRENOMS_ACCESSOR, withRemarqueOnChange, DEFAULT_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, NomPrenom.extractPrenomUsuel(src.getPrenoms()), PRENOM_USUEL_ACCESSOR, withRemarqueOnChange, DEFAULT_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getCategorieEtranger(), CATEGORIE_ETRANGER_ACCESSOR, withRemarqueOnChange, CATEGORIE_ETRANGER_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getDateDeces(), DATE_DECES_ACCESSOR, withRemarqueOnChange, DATE_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getDateNaissance(), DATE_NAISSANCE_ACCESSOR, withRemarqueOnChange, DATE_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getNomMere(), NOM_MERE_ACCESSOR, withRemarqueOnChange, DEFAULT_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getPrenomsMere(), PRENOMS_MERE_ACCESSOR, withRemarqueOnChange, DEFAULT_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getNomPere(), NOM_PERE_ACCESSOR, withRemarqueOnChange, DEFAULT_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getPrenomsPere(), PRENOMS_PERE_ACCESSOR, withRemarqueOnChange, DEFAULT_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getAvs(), NO_AVS_ACCESSOR, withRemarqueOnChange, AVS_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getOfsPaysNationalite(), NATIONALITE_ACCESSOR, withRemarqueOnChange, PAYS_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getOrigine(), ORIGINE_ACCESSOR, withRemarqueOnChange, ORIGINE_RENDERER));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getSexe(), SEXE_ACCESSOR, withRemarqueOnChange, SEXE_RENDERER));
		return elementsRemarque;
	}

	private void addRemarqueCreation(Contribuable ctb, EvenementReqDes evt) {
		final String texte = buildTexteRemarqueCreation(evt);
		addRemarque(ctb, texte);
	}

	private void addRemarqueModification(Contribuable ctb, List<String> elements, EvenementReqDes evt) {
		final String texte = buildTexteRemarqueModification(elements, evt);
		addRemarque(ctb, texte);
	}

	private void addRemarque(Contribuable ctb, String texte) {
		final String abb = StringUtils.abbreviate(texte, LengthConstants.TIERS_REMARQUE);
		final Remarque rq = new Remarque();
		rq.setTiers(ctb);
		rq.setTexte(abb);
		hibernateTemplate.merge(rq);
	}

	private static String buildTexteRemarqueCreation(EvenementReqDes evt) {
		final StringBuilder b = new StringBuilder();
		b.append("Contribuable créé le ").append(RegDateHelper.dateToDisplayString(RegDate.get()));
		b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(evt.getDateActe()));
		b.append(" par le notaire ").append(buildActeurString(evt.getNotaire()));
		b.append(" et enregistré par ");
		if (evt.getOperateur() == null) {
			b.append("lui-même");
		}
		else {
			b.append(buildActeurString(evt.getOperateur()));
		}
		b.append(".");
		return b.toString();
	}

	private static String buildTexteRemarqueModification(List<String> elements, EvenementReqDes evt) {
		final StringBuilder b = new StringBuilder();
		b.append("Contribuable mis à jour le ").append(RegDateHelper.dateToDisplayString(RegDate.get()));
		b.append(" par l'acte notarial du ").append(RegDateHelper.dateToDisplayString(evt.getDateActe()));
		b.append(" par le notaire ").append(buildActeurString(evt.getNotaire()));
		b.append(" et enregistré par ");
		if (evt.getOperateur() == null) {
			b.append("lui-même");
		}
		else {
			b.append(buildActeurString(evt.getOperateur()));
		}
		b.append(".");
		for (String elt : elements) {
			b.append("\n- ").append(elt);
		}
		return b.toString();
	}

	private static String buildActeurString(InformationsActeur acteur) {
		final NomPrenom nomPrenom = new NomPrenom(acteur.getNom(), acteur.getPrenom());
		return String.format("%s (%s)", nomPrenom.getNomPrenom(), acteur.getVisa());
	}

	private static <T> String updateAttribute(PersonnePhysique pp, T newValue, AttributeAccessor<T> accessor, boolean withRemarqueOnChange, StringRenderer<? super T> renderer) {
		final T oldValue = accessor.get(pp);
		if (oldValue != newValue && (oldValue == null || !oldValue.equals(newValue))) {
			accessor.set(pp, newValue);
			return withRemarqueOnChange ? String.format("%s : %s -> %s", accessor.getAttributeDisplayName(), renderer.toString(oldValue), renderer.toString(newValue)) : null;
		}
		return null;
	}

	private static void addRemarqueElement(List<String> elts, String updateRemarkPart) {
		if (StringUtils.isNotBlank(updateRemarkPart)) {
			elts.add(updateRemarkPart);
		}
	}

	private static final String VIDE = "vide";

	private static final StringRenderer<Object> DEFAULT_RENDERER = value -> value == null ? VIDE : String.format("\"%s\"", value);

	private static final StringRenderer<RegDate> DATE_RENDERER = value -> value == null ? VIDE : RegDateHelper.dateToDisplayString(value);

	private static final StringRenderer<EtatCivil> ETAT_CIVIL_RENDERER = value -> value == null ? VIDE : value.format();

	private static final StringRenderer<String> AVS_RENDERER = value -> value == null ? VIDE : FormatNumeroHelper.formatNumAVS(value);

	private static final StringRenderer<Sexe> SEXE_RENDERER = value -> value == null ? VIDE : value.getDisplayName();

	private static final StringRenderer<CategorieEtranger> CATEGORIE_ETRANGER_RENDERER = value -> value == null ? VIDE : value.getDisplayName();

	private final StringRenderer<Integer> PAYS_RENDERER = new StringRenderer<Integer>() {
		@Override
		public String toString(Integer value) {
			if (value == null) {
				return VIDE;
			}
			final Pays pays = infraService.getPays(value, null);
			return pays == null ? value.toString() : pays.getNomCourt();
		}
	};

	private static final StringRenderer<OriginePersonnePhysique> ORIGINE_RENDERER = origine -> origine == null ? VIDE : origine.getLibelleAvecCanton();

	private static final AttributeAccessor<String> NOM_ACCESSOR = new AttributeAccessor<String>() {
		@Override
		public String get(PersonnePhysique pp) {
			return pp.getNom();
		}

		@Override
		public void set(PersonnePhysique pp, String value) {
			pp.setNom(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Nom";
		}
	};

	private static final AttributeAccessor<String> NOM_NAISSANCE_ACCESSOR = new AttributeAccessor<String>() {
		@Override
		public String get(PersonnePhysique pp) {
			return pp.getNomNaissance();
		}

		@Override
		public void set(PersonnePhysique pp, String value) {
			pp.setNomNaissance(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Nom de naissance";
		}
	};

	private static final AttributeAccessor<String> PRENOMS_ACCESSOR = new AttributeAccessor<String>() {
		@Override
		public String get(PersonnePhysique pp) {
			return pp.getTousPrenoms();
		}

		@Override
		public void set(PersonnePhysique pp, String value) {
			pp.setTousPrenoms(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Prénoms";
		}
	};

	private static final AttributeAccessor<String> PRENOM_USUEL_ACCESSOR = new AttributeAccessor<String>() {
		@Override
		public String get(PersonnePhysique pp) {
			return pp.getPrenomUsuel();
		}

		@Override
		public void set(PersonnePhysique pp, String value) {
			pp.setPrenomUsuel(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Prénom usuel";
		}
	};

	private static final AttributeAccessor<CategorieEtranger> CATEGORIE_ETRANGER_ACCESSOR = new AttributeAccessor<CategorieEtranger>() {
		@Override
		public CategorieEtranger get(PersonnePhysique pp) {
			return pp.getCategorieEtranger();
		}

		@Override
		public void set(PersonnePhysique pp, CategorieEtranger value) {
			pp.setCategorieEtranger(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Catégorie d'étranger";
		}
	};

	private static final AttributeAccessor<OriginePersonnePhysique> ORIGINE_ACCESSOR = new AttributeAccessor<OriginePersonnePhysique>() {
		@Override
		public OriginePersonnePhysique get(PersonnePhysique pp) {
			return pp.getOrigine();
		}

		@Override
		public void set(PersonnePhysique pp, OriginePersonnePhysique value) {
			pp.setOrigine(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Origine";
		}
	};

	private static final AttributeAccessor<RegDate> DATE_DECES_ACCESSOR = new AttributeAccessor<RegDate>() {
		@Override
		public RegDate get(PersonnePhysique pp) {
			return pp.getDateDeces();
		}

		@Override
		public void set(PersonnePhysique pp, RegDate value) {
			pp.setDateDeces(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Date de décès";
		}
	};

	private static final AttributeAccessor<RegDate> DATE_NAISSANCE_ACCESSOR = new AttributeAccessor<RegDate>() {
		@Override
		public RegDate get(PersonnePhysique pp) {
			return pp.getDateNaissance();
		}

		@Override
		public void set(PersonnePhysique pp, RegDate value) {
			pp.setDateNaissance(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Date de naissance";
		}
	};

	private static final AttributeAccessor<String> NOM_MERE_ACCESSOR = new AttributeAccessor<String>() {
		@Override
		public String get(PersonnePhysique pp) {
			return pp.getNomMere();
		}

		@Override
		public void set(PersonnePhysique pp, String value) {
			pp.setNomMere(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Nom de la mère";
		}
	};

	private static final AttributeAccessor<String> PRENOMS_MERE_ACCESSOR = new AttributeAccessor<String>() {
		@Override
		public String get(PersonnePhysique pp) {
			return pp.getPrenomsMere();
		}

		@Override
		public void set(PersonnePhysique pp, String value) {
			pp.setPrenomsMere(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Prénoms de la mère";
		}
	};

	private static final AttributeAccessor<String> NOM_PERE_ACCESSOR = new AttributeAccessor<String>() {
		@Override
		public String get(PersonnePhysique pp) {
			return pp.getNomPere();
		}

		@Override
		public void set(PersonnePhysique pp, String value) {
			pp.setNomPere(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Nom du père";
		}
	};

	private static final AttributeAccessor<String> PRENOMS_PERE_ACCESSOR = new AttributeAccessor<String>() {
		@Override
		public String get(PersonnePhysique pp) {
			return pp.getPrenomsPere();
		}

		@Override
		public void set(PersonnePhysique pp, String value) {
			pp.setPrenomsPere(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Prénoms du père";
		}
	};

	private static final AttributeAccessor<String> NO_AVS_ACCESSOR = new AttributeAccessor<String>() {
		@Override
		public String get(PersonnePhysique pp) {
			return pp.getNumeroAssureSocial();
		}

		@Override
		public void set(PersonnePhysique pp, String value) {
			pp.setNumeroAssureSocial(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "NAVS13";
		}
	};

	private static final AttributeAccessor<Integer> NATIONALITE_ACCESSOR = new AttributeAccessor<Integer>() {
		@Override
		public Integer get(PersonnePhysique pp) {
			return pp.getNumeroOfsNationalite();
		}

		@Override
		public void set(PersonnePhysique pp, Integer value) {
			pp.setNumeroOfsNationalite(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Nationalité";
		}
	};

	private static final AttributeAccessor<Sexe> SEXE_ACCESSOR = new AttributeAccessor<Sexe>() {
		@Override
		public Sexe get(PersonnePhysique pp) {
			return pp.getSexe();
		}

		@Override
		public void set(PersonnePhysique pp, Sexe value) {
			pp.setSexe(value);
		}

		@Override
		public String getAttributeDisplayName() {
			return "Sexe";
		}
	};
}
