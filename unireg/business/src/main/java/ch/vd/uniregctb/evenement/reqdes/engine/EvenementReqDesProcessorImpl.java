package ch.vd.uniregctb.evenement.reqdes.engine;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BlockingQueuePollingThread;
import ch.vd.uniregctb.common.Dated;
import ch.vd.uniregctb.hibernate.HibernateTemplate;
import ch.vd.uniregctb.reqdes.ErreurTraitement;
import ch.vd.uniregctb.reqdes.EtatTraitement;
import ch.vd.uniregctb.reqdes.EvenementReqDes;
import ch.vd.uniregctb.reqdes.InformationsActeur;
import ch.vd.uniregctb.reqdes.PartiePrenante;
import ch.vd.uniregctb.reqdes.UniteTraitement;
import ch.vd.uniregctb.reqdes.UniteTraitementDAO;
import ch.vd.uniregctb.transaction.TransactionManager;
import ch.vd.uniregctb.transaction.TransactionTemplate;

/**
 * Traitement des unités de traitement issues des événements eReqDes
 */
public class EvenementReqDesProcessorImpl implements EvenementReqDesProcessor, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(EvenementReqDesProcessorImpl.class);

	private static final Set<EtatTraitement> ETATS_FINAUX = EnumSet.of(EtatTraitement.A_VERIFIER, EtatTraitement.FORCE, EtatTraitement.TRAITE);

	private final BlockingQueue<QueueElement> queue = new LinkedBlockingQueue<>();

	private WorkerThread workerThread;

	private TransactionManager transactionManager;
	private HibernateTemplate hibernateTemplate;
	private UniteTraitementDAO uniteTraitementDAO;

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
	}

	public void setTransactionManager(TransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	public void setUniteTraitementDAO(UniteTraitementDAO uniteTraitementDAO) {
		this.uniteTraitementDAO = uniteTraitementDAO;
	}

	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * @return un timestamp en nano-secondes
	 */
	private static long getTimestamp() {
		return System.nanoTime();
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
		catch (final Exception ex) {
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
	 * Effectue les contrôles préliminaires sur l'état des données entrantes, en mettant les erreurs/warnings dans l'unité de traitement directement si nécessaire
	 * @param ut unité de traitement à contrôler
	 * @throws EvenementReqDesException s'il vaut mieux tout arrêter tant la situation est grave
	 */
	private void doControlesPreliminaires(UniteTraitement ut) throws EvenementReqDesException {
		// TODO à faire
	}

	/**
	 * Travail de mise à jour du modèle de données des contribuables par rapports aux données reçues
	 * @param evt événement reçu de eReqDes
	 * @param partiesPrenantes liste des parties prenantes de l'unité de traitement
	 * @throws EvenementReqDesException en cas d'erreur
	 */
	private void doProcessing(EvenementReqDes evt, Set<PartiePrenante> partiesPrenantes) throws EvenementReqDesException {
		// TODO à faire
	}
}
