package ch.vd.uniregctb.evenement.reqdes.engine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.infra.data.Commune;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BlockingQueuePollingThread;
import ch.vd.uniregctb.common.Dated;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;
import ch.vd.uniregctb.reqdes.ErreurTraitement;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.reqdes.EvenementReqDes;
import ch.vd.uniregctb.reqdes.InformationsActeur;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.RolePartiePrenante;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.reqdes.UniteTraitementDAO;
import ch.vd.uniregctb.transaction.TransactionTemplate;
import ch.vd.uniregctb.type.EtatCivil;

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
						ut.getErreurs().clear();
						processUniteTraitement(ut);         // <-- c'est ici que tout est fait

						final EtatTraitement nouvelEtat = computeNouvelEtat(ut.getErreurs());
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
			LOGGER.error("Exception lors du traitement de l'unité de traitement %d", e);

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

	private static EtatTraitement computeNouvelEtat(Set<ErreurTraitement> erreurs) {
		if (erreurs != null && !erreurs.isEmpty()) {
			// oups ! -> en erreur ou à vérifier
			boolean reelleErreur = false;
			for (ErreurTraitement erreur : erreurs) {
				if (erreur.getType() == ErreurTraitement.TypeErreur.ERROR) {
					reelleErreur = true;
					break;
				}
			}
			return reelleErreur ? EtatTraitement.EN_ERREUR : EtatTraitement.A_VERIFIER;
		}
		else {
			return EtatTraitement.TRAITE;
		}
	}

	private static String extractVisaPrincipal(EvenementReqDes evt) {
		final InformationsActeur acteur = evt.getOperateur() != null ? evt.getOperateur() : evt.getNotaire();
		return acteur.getVisa();
	}

	private void processUniteTraitement(UniteTraitement ut) throws EvenementReqDesException {
		// Deux étapes :
		// 1. étape de contrôle : l'idée est que l'on ne modifie rien en base dans cette phase, et que les erreurs sont remontées dans une liste
		// 2. si la première étape est passée sans erreur, on peut commencer les modifications -> toute erreur levée à ce stade sera l'objet d'une exception
		doControlesPreliminaires(ut);
		if (isEmptyOrContainsOnlyWarnings(ut.getErreurs())) {
			// on continue
			doProcessing(ut.getEvenement(), ut.getPartiesPrenantes());
		}
		else {
			// log des erreurs avant fin des travaux
			final StringBuilder b = new StringBuilder("Erreur(s) rencontrée(s) lors du contrôle de cohérence de l'unité de traitement ").append(ut.getId()).append(" :");
			for (ErreurTraitement erreur : ut.getErreurs()) {
				b.append("\n - <").append(erreur.getType()).append("> ").append(erreur.getMessage());
			}
			LOGGER.error(b.toString());
		}
	}

	private static boolean isEmptyOrContainsOnlyWarnings(Collection<ErreurTraitement> erreurs) {
		if (erreurs.isEmpty()) {
			return true;
		}
		boolean hasErreur = false;
		for (ErreurTraitement erreur : erreurs) {
			if (erreur.getType() == ErreurTraitement.TypeErreur.ERROR) {
				hasErreur = true;
				break;
			}
		}
		return !hasErreur;
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
	 * Effectue les contrôles préliminaires sur l'état des données entrantes, en mettant les erreurs/warnings dans l'unité de traitement directement si nécessaire
	 * @param ut unité de traitement à contrôler
	 * @throws EvenementReqDesException s'il vaut mieux tout arrêter tant la situation est grave
	 */
	private void doControlesPreliminaires(UniteTraitement ut) throws EvenementReqDesException {
		final RegDate dateActe = ut.getEvenement().getDateActe();
		final Set<ErrorInfo> protoErreurs = new HashSet<>();      // pour éviter les doublons d'erreurs

		// vérification de la date de l'acte, qui ne doit pas être dans le futur
		if (RegDate.get().isBefore(dateActe)) {
			protoErreurs.add(new ErrorInfo(ErreurTraitement.TypeErreur.ERROR, "La date de l'acte est dans le futur."));
		}

		final Set<EtatCivil> etatsCivilsSansConjoint = EnumSet.of(EtatCivil.CELIBATAIRE, EtatCivil.VEUF, EtatCivil.DIVORCE, EtatCivil.NON_MARIE,
		                                                          EtatCivil.PARTENARIAT_DISSOUS_DECES, EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT);

		for (PartiePrenante pp : ut.getPartiesPrenantes()) {
			final Integer ofsCommuneDomicile = pp.getOfsCommune();
			if (ofsCommuneDomicile != null) {
				// problématique des fractions (-> les fors ne peuvent pas être ouverts sur les communes faîtières de fractions)
				final Commune commune = infraService.getCommuneByNumeroOfs(ofsCommuneDomicile, dateActe);
				protoErreurs.addAll(getErreursTraitementPourCommune(ofsCommuneDomicile, dateActe, commune));

				// la commune de résidence ne doit pas être vaudoise pour une mise à jour automatique
				if (commune != null && commune.isVaudoise()) {
					protoErreurs.add(new ErrorInfo(ErreurTraitement.TypeErreur.ERROR, String.format("La commune de résidence (%s/%d) est vaudoise.", commune.getNomOfficiel(), ofsCommuneDomicile)));
				}
			}

			// problématique des fractions (-> les fors ne peuvent pas être ouverts sur les communes faîtières de fractions)
			for (RolePartiePrenante role : pp.getRoles()) {
				final int ofsCommuneImmeuble = role.getTransaction().getOfsCommune();
				final Commune commune = infraService.getCommuneByNumeroOfs(ofsCommuneImmeuble, dateActe);
				protoErreurs.addAll(getErreursTraitementPourCommune(ofsCommuneImmeuble, dateActe, commune));
			}

			// vérification qu'une partie prenante célibataire (ou assimilée comme telle) n'est pas indiquée avec un conjoint
			final EtatCivil etatCivil = pp.getEtatCivil();
			if (etatsCivilsSansConjoint.contains(etatCivil)) {
				if (pp.getConjointPartiePrenante() != null || StringUtils.isNotBlank(pp.getNomConjoint()) || StringUtils.isNotBlank(pp.getPrenomConjoint())) {
					protoErreurs.add(new ErrorInfo(ErreurTraitement.TypeErreur.ERROR, String.format("Incohérence entre l'état civil (%s) et la présence d'un conjoint.", etatCivil.format())));
				}
			}

			// vérification que les liens sont bien bi-directionnels
			if (pp.getConjointPartiePrenante() != null && pp.getConjointPartiePrenante().getConjointPartiePrenante() != pp) {
				protoErreurs.add(new ErrorInfo(ErreurTraitement.TypeErreur.ERROR, "Liens matrimoniaux incohérents entres les parties prenantes."));
			}
		}

		// recopie des erreurs collectées dans l'unité de traitement
		if (!protoErreurs.isEmpty()) {
			final Set<ErreurTraitement> erreurs = ut.getErreurs();
			for (ErrorInfo info : protoErreurs) {
				erreurs.add(new ErreurTraitement(info.typeErreur, info.message));
			}
		}
	}

	private static List<ErrorInfo> getErreursTraitementPourCommune(int noOfsCommune, RegDate dateActe, Commune commune) throws EvenementReqDesException {
		final List<ErrorInfo> erreurs = new ArrayList<>(1);
		if (commune == null) {
			erreurs.add(new ErrorInfo(ErreurTraitement.TypeErreur.ERROR, String.format("Commune %d inconnue au %s.", noOfsCommune, RegDateHelper.dateToDisplayString(dateActe))));
		}
		else if (commune.isPrincipale()) {
			erreurs.add(new ErrorInfo(ErreurTraitement.TypeErreur.ERROR, String.format("La commune '%s' (%d) est une commune faîtière de fractions.", commune.getNomOfficiel(), noOfsCommune)));
		}
		return erreurs;
	}

	/**
	 * Travail de mise à jour du modèle de données des contribuables par rapports aux données reçues
	 * @param evt événement reçu de eReqDes
	 * @param partiesPrenantes liste des parties prenantes de l'unité de traitement
	 * @throws EvenementReqDesException en cas d'erreur
	 */
	private void doProcessing(EvenementReqDes evt, Set<PartiePrenante> partiesPrenantes) throws EvenementReqDesException {
		// TODO à faire
		throw new NotImplementedException("Traitement non implémenté");
	}
}
