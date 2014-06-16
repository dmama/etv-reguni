package ch.vd.uniregctb.evenement.reqdes.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.common.NomPrenom;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.unireg.interfaces.infra.data.Localite;
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
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
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
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.reqdes.UniteTraitementDAO;
import ch.vd.uniregctb.tiers.Contribuable;
import ch.vd.uniregctb.tiers.EnsembleTiersCouple;
import ch.vd.uniregctb.tiers.ForFiscalPrincipal;
import ch.vd.uniregctb.tiers.PersonnePhysique;
import ch.vd.uniregctb.tiers.Remarque;
import ch.vd.uniregctb.tiers.Tiers;
import ch.vd.uniregctb.tiers.TiersService;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.CategorieEtranger;
import ch.vd.uniregctb.type.EtatCivil;
import ch.vd.uniregctb.type.Sexe;
import ch.vd.uniregctb.type.TexteCasePostale;
import ch.vd.uniregctb.type.TypeAdresseTiers;
import ch.vd.uniregctb.type.TypeAutoriteFiscale;

/**
 * Traitement des unités de traitement issues des événements eReqDes
 */
public class EvenementReqDesProcessorImpl implements EvenementReqDesProcessor, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(EvenementReqDesProcessorImpl.class);

	private static final Set<EtatTraitement> ETATS_FINAUX = EnumSet.of(EtatTraitement.A_VERIFIER, EtatTraitement.FORCE, EtatTraitement.TRAITE);

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

	/**
	 * Handle utilisé pour les listeners de traitement
	 */
	private static class HandleImpl implements ListenerHandle {
		private static final AtomicLong SEQUENCE = new AtomicLong(0L);
		private final long id;
		private HandleImpl() {
			id = SEQUENCE.getAndIncrement();
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

	/**
	 * @return un timestamp en nano-secondes
	 */
	private static long getTimestamp() {
		return System.nanoTime();
	}

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

	@Override
	public void unregisterListener(ListenerHandle handle) {
		if (!(handle instanceof HandleImpl)) {
			throw new IllegalArgumentException("Invalid handle");
		}
		synchronized (listeners) {
			if (listeners.remove(((HandleImpl) handle).id) == null) {
				throw new IllegalArgumentException("Unknown handle");
			}
		}
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
						listener.onUniteTraite(idUniteTraitement);
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

							final StringBuilder b = new StringBuilder("Erreur(s) rencontrée(s) lors du contrôle de cohérence de l'unité de traitement ").append(ut.getId()).append(" :");

							// recopie des erreurs collectées dans l'unité de traitement
							for (ErrorInfo info : infos) {
								b.append("\n- <").append(info.typeErreur).append("> ").append(info.message);
								erreurs.add(new ErreurTraitement(info.typeErreur, info.message));
							}

	                        LOGGER.error(b.toString());
						}

						final EtatTraitement nouvelEtat = errorCollector.hasCollectedMessages()
								? EtatTraitement.EN_ERREUR
								: (warningCollector.hasCollectedMessages() ? EtatTraitement.A_VERIFIER : EtatTraitement.TRAITE);
						if (LOGGER.isInfoEnabled()) {
							LOGGER.info(String.format("Traitement de l'unité de traitement %d terminé dans l'état %s", idUniteTraitement, nouvelEtat));
						}
						ut.setDateTraitement(DateHelper.getCurrentDate());
						ut.setEtat(nouvelEtat);

						// un petit flush avant de partir (histoire que les visas utilisés soient les bons)
						hibernateTemplate.flush();
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

					final Set<ErreurTraitement> erreurs = ut.getErreurs();
					erreurs.clear();
					erreurs.add(erreur);
				}
			});
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
	 * @throws EvenementReqDesException s'il vaut mieux tout arrêter tant la situation est grave
	 */
	private void doControlesPreliminaires(UniteTraitement ut, MessageCollector errorCollector, MessageCollector warningCollector) throws EvenementReqDesException {
		final RegDate dateActe = ut.getEvenement().getDateActe();

		// vérification de la date de l'acte, qui ne doit pas être dans le futur
		if (RegDate.get().isBefore(dateActe)) {
			errorCollector.addNewMessage("La date de l'acte est dans le futur.");
		}

		final Set<EtatCivil> etatsCivilsSansConjoint = EnumSet.of(EtatCivil.CELIBATAIRE, EtatCivil.VEUF, EtatCivil.DIVORCE, EtatCivil.NON_MARIE,
		                                                          EtatCivil.PARTENARIAT_DISSOUS_DECES, EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT);

		for (PartiePrenante pp : ut.getPartiesPrenantes()) {
			final Integer ofsCommuneDomicile = pp.getOfsCommune();
			if (ofsCommuneDomicile != null) {
				// problématique des fractions (-> les fors ne peuvent pas être ouverts sur les communes faîtières de fractions)
				final Commune commune = infraService.getCommuneByNumeroOfs(ofsCommuneDomicile, dateActe);
				checkCommune(ofsCommuneDomicile, dateActe, commune, errorCollector);

				// la commune de résidence ne doit pas être vaudoise pour une mise à jour automatique
				if (commune != null && commune.isVaudoise()) {
					errorCollector.addNewMessage(String.format("La commune de résidence (%s/%d) est vaudoise.", commune.getNomOfficiel(), ofsCommuneDomicile));
				}
			}

			// problématique des fractions (-> les fors ne peuvent pas être ouverts sur les communes faîtières de fractions)
			for (RolePartiePrenante role : pp.getRoles()) {
				final int ofsCommuneImmeuble = role.getTransaction().getOfsCommune();
				final Commune commune = infraService.getCommuneByNumeroOfs(ofsCommuneImmeuble, dateActe);
				checkCommune(ofsCommuneImmeuble, dateActe, commune, errorCollector);
			}

			// vérification qu'une partie prenante célibataire (ou assimilée comme telle) n'est pas indiquée avec un conjoint
			final EtatCivil etatCivil = pp.getEtatCivil();
			if (etatsCivilsSansConjoint.contains(etatCivil)) {
				if (pp.getConjointPartiePrenante() != null || StringUtils.isNotBlank(pp.getNomConjoint()) || StringUtils.isNotBlank(pp.getPrenomConjoint())) {
					errorCollector.addNewMessage(String.format("Incohérence entre l'état civil (%s) et la présence d'un conjoint.", etatCivil.format()));
				}
			}

			// vérification que les liens sont bien bi-directionnels
			if (pp.getConjointPartiePrenante() != null && pp.getConjointPartiePrenante().getConjointPartiePrenante() != pp) {
				errorCollector.addNewMessage("Liens matrimoniaux incohérents entres les parties prenantes.");
			}
		}
	}

	private static void checkCommune(int noOfsCommune, RegDate dateActe, Commune commune, MessageCollector errorCollector) throws EvenementReqDesException {
		if (commune == null) {
			errorCollector.addNewMessage(String.format("Commune %d inconnue au %s.", noOfsCommune, RegDateHelper.dateToDisplayString(dateActe)));
		}
		else if (commune.isPrincipale()) {
			errorCollector.addNewMessage(String.format("La commune '%s' (%d) est une commune faîtière de fractions.", commune.getNomOfficiel(), noOfsCommune));
		}
	}

	/**
	 * Travail de mise à jour du modèle de données des contribuables par rapports aux données reçues
	 * @param evt événement reçu de eReqDes
	 * @param partiesPrenantes liste des parties prenantes de l'unité de traitement
	 * @param warningCollector collecteurs d'avertissements
	 * @throws EvenementReqDesException en cas d'erreur
	 */
	private void doProcessing(EvenementReqDes evt, Set<PartiePrenante> partiesPrenantes, MessageCollector warningCollector) throws EvenementReqDesException {

		final RegDate dateActe = evt.getDateActe();

		// la clé est l'ID de la partie prenante...
		final Map<Long, PersonnePhysique> personnesPhysiques = new HashMap<>(partiesPrenantes.size());
		for (PartiePrenante ppSrc : partiesPrenantes) {
			final PersonnePhysique ppDest;
			if (ppSrc.isSourceCivile()) {
				// on ne fait rien sur une partie prenante qui vient du civil
				ppDest = null;
			}
			else if (ppSrc.getNumeroContribuable() != null) {
				final Tiers tiers = tiersService.getTiers(ppSrc.getNumeroContribuable());
				if (tiers == null) {
					throw new EvenementReqDesException(String.format("Le numéro de contribuable %s ne correspond à rien de connu.", FormatNumeroHelper.numeroCTBToDisplay(ppSrc.getNumeroContribuable())));
				}
				if (!(tiers instanceof PersonnePhysique)) {
					throw new EvenementReqDesException(String.format("Le tiers %s n'est pas une personne physique.", FormatNumeroHelper.numeroCTBToDisplay(ppSrc.getNumeroContribuable())));
				}
				ppDest = (PersonnePhysique) tiers;

				// vérification que l'on n'est pas en train de vouloir modifier un Vaudois
				checkForEtResidenceAvantModification(ppDest, dateActe);

				// mise à jour des informations de base et premiers éléments constitutifs de la remarque éventuelle à ajouter au tiers
				final List<String> elementsRemarque = updatePersonnePhysiqueConnue(ppSrc, ppDest);

				// mise à jour de l'adresse courrier (ou addition d'une nouvelle remarque)
				elementsRemarque.addAll(updateAdresse(ppSrc, ppDest, dateActe, warningCollector));

				// construction d'une remarque avec les données déjà récoltées
				if (!elementsRemarque.isEmpty()) {
					addRemarqueModification(ppDest, elementsRemarque, evt);
				}
			}
			else {
				ppDest = createPersonnePhysique(ppSrc);
				addAdresseCourrier(ppSrc, ppDest, dateActe);
				addRemarqueCreation(ppDest, evt);
			}

			// on collecte les personnes physiques concernées
			personnesPhysiques.put(ppSrc.getId(), ppDest);
		}

		// TODO reste à mettre à jour les états civils et les éventuels liens d'appartenance ménage

		// TODO reste enfin à metre à jour les fors fiscaux
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
			a.setNumeroOrdrePoste(pp.getNumeroOrdrePostal() != null ? pp.getNumeroOrdrePostal() : findNumeroOrdrePostal(pp.getId(), pp.getLocalite(), pp.getNumeroPostal(), pp.getNumeroPostalComplementaire()));
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

	private Integer findNumeroOrdrePostal(long idPartiePrenante, String localite, String npaString, Integer npaComplement) throws EvenementReqDesException {
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
			if (candidate.isValide()
					&& (npa == null || npa.equals(candidate.getNPA()))
					&& candidate.getNomCompletMinuscule().equalsIgnoreCase(localite)
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
				b.append(l.getNomCompletMinuscule()).append(" (").append(l.getNoOrdre()).append(") ");
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

				// pour mettre l'unité de traitement au mieux dans l'état "A_VERIFIER"
				warningCollector.addNewMessage(String.format("Adresse non modifiée sur un contribuable assujetti, uniquement reprise dans les remarques du tiers."));

				// ce texte sera inclu dans la remarque
				return Arrays.asList(b.toString());
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
		if (isAssujettisRoleOrdinaire(ppDest, dateDebut)) {
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

	private static List<String> dumpBaseDataToPersonnePhysique(PartiePrenante src, PersonnePhysique dest, boolean withRemarqueOnChange) {
		final List<String> elementsRemarque = new LinkedList<>();
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getNom(), NOM_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getPrenoms(), PRENOMS_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, extractPrenomUsuel(src.getPrenoms()), PRENOM_USUEL_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getCategorieEtranger(), CATEGORIE_ETRANGER_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getDateDeces(), DATE_DECES_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getDateNaissance(), DATE_NAISSANCE_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getNomMere(), NOM_MERE_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getPrenomsMere(), PRENOMS_MERE_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getNomPere(), NOM_PERE_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getPrenomsPere(), PRENOMS_PERE_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getAvs(), NO_AVS_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getOfsPaysNationalite(), NATIONALITE_ACCESSOR, withRemarqueOnChange));
		addRemarqueElement(elementsRemarque, updateAttribute(dest, src.getSexe(), SEXE_ACCESSOR, withRemarqueOnChange));
		return elementsRemarque;
	}

	private static String extractPrenomUsuel(String prenoms) {
		if (StringUtils.isBlank(prenoms)) {
			return null;
		}
		final String[] parts = prenoms.trim().split("\\s");
		return parts[0];
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

	private static <T> String updateAttribute(PersonnePhysique pp, T newValue, AttributeAccessor<T> accessor, boolean withRemarqueOnChange) {
		final T oldValue = accessor.get(pp);
		if (oldValue != newValue && (oldValue == null || !oldValue.equals(newValue))) {
			accessor.set(pp, newValue);
			return withRemarqueOnChange ? String.format("%s : %s -> %s", accessor.getAttributeDisplayName(), toString(oldValue), toString(newValue)) : null;
		}
		return null;
	}

	private static void addRemarqueElement(List<String> elts, String updateRemarkPart) {
		if (StringUtils.isNotBlank(updateRemarkPart)) {
			elts.add(updateRemarkPart);
		}
	}

	private static <T> String toString(T value) {
		if (value == null) {
			return "vide";
		}
		if (value instanceof RegDate) {
			return RegDateHelper.dateToDisplayString((RegDate) value);
		}
		if (value instanceof Enum || value instanceof Number) {
			return value.toString();
		}
		return String.format("\"%s\"", value);
	}

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
