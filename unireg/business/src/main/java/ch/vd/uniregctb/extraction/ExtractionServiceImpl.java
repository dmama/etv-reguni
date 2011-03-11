package ch.vd.uniregctb.extraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.registre.base.utils.Assert;
import ch.vd.uniregctb.common.AuthenticationHelper;
import ch.vd.uniregctb.common.BatchResults;
import ch.vd.uniregctb.common.BatchTransactionTemplate;
import ch.vd.uniregctb.common.ParallelBatchTransactionTemplate;
import ch.vd.uniregctb.common.StatusManager;
import ch.vd.uniregctb.inbox.InboxAttachment;
import ch.vd.uniregctb.inbox.InboxService;

/**
 * Implémentation du service d'extractions asynchrones
 */
public class ExtractionServiceImpl implements ExtractionService, ExtractionServiceMonitoring, InitializingBean, DisposableBean {

	private static final Logger LOGGER = Logger.getLogger(ExtractionServiceImpl.class);

	private static final long NANOS_IN_MILLI = TimeUnit.MILLISECONDS.toNanos(1);

	/**
	 * Valeur, en jours, de l'expiration de l'extraction...
	 */
	private int expiration = 3;

	/**
	 * Taille du pool de threads des exécuteurs
	 */
	private int threadPoolSize = 1;

	/**
	 * Transaction manager
	 */
	private PlatformTransactionManager transactionManager;

	/**
	 * Hibernate template
	 */
	private HibernateTemplate hibernateTemplate;

	/**
	 * Service de gestion des documents une fois l'extraction terminée
	 */
	private InboxService inboxService;

	/**
	 * Liste des exécuteurs
	 */
	private List<Executor> executors;

	/**
	 * Queue partagée par les exécuteurs
	 */
	private final BlockingQueue<ExtractionJobImpl> queue = new LinkedBlockingQueue<ExtractionJobImpl>();

	/**
	 * Nombre de jobs terminés depuis le démarrage du service
	 */
	private final AtomicInteger nbJobsTermines = new AtomicInteger(0);

	@SuppressWarnings({"UnusedDeclaration"})
	public void setExpiration(int expiration) {
		this.expiration = expiration;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setThreadPoolSize(int threadPoolSize) {
		this.threadPoolSize = threadPoolSize;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setTransactionManager(PlatformTransactionManager transactionManager) {
		this.transactionManager = transactionManager;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setHibernateTemplate(HibernateTemplate hibernateTemplate) {
		this.hibernateTemplate = hibernateTemplate;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInboxService(InboxService inboxService) {
		this.inboxService = inboxService;
	}

	private static enum JobState {
		/**
		 * Premier état : en attente de démarrage
		 */
		WAITING_FOR_START,

		/**
		 * Annulé avant même d'avoir commencé
		 */
		CANCELLED,

		/**
		 * En cours
		 */
		RUNNING,

		/**
		 * Arrêté
		 */
		STOPPED
	}

	/**
	 * Classe de description d'un job tout au long de sa vie
	 */
	private final class ExtractionJobImpl implements ExtractionJob {

		private final ExtractionKey key;
		private final ExtractorLauncher extractor;
		private final Date creationDate;
		private Long startTimestamp;
		private Long endTimestamp;
		private JobState state;
		private ExtractionResult result;

		public ExtractionJobImpl(ExtractionKey key, ExtractorLauncher extractor) {
			this.key = key;
			this.extractor = extractor;
			this.state = JobState.WAITING_FOR_START;
			this.startTimestamp = null;
			this.endTimestamp = null;
			this.creationDate = DateHelper.getCurrentDate();
		}

		public ExtractionResult getResult() {
			return result;
		}

		public void onStart() {
			Assert.isEqual(JobState.WAITING_FOR_START, this.state);
			this.state = JobState.RUNNING;
			this.startTimestamp = System.nanoTime();
		}

		public void onStop(ExtractionResult result) {
			Assert.isTrue(this.state != JobState.STOPPED && this.state != JobState.CANCELLED);
			if (this.state == JobState.RUNNING) {
				this.state = JobState.STOPPED;
			}
			else if (this.state == JobState.WAITING_FOR_START) {
				this.state = JobState.CANCELLED;
			}
			this.result = result;
			this.endTimestamp = System.nanoTime();
		}

		public ExtractionKey getKey() {
			return key;
		}

		public ExtractorLauncher getExtractor() {
			return extractor;
		}

		public boolean isRunning() {
			return state == JobState.RUNNING;
		}

		public Date getCreationDate() {
			return creationDate;
		}

		/**
		 * @return la durée d'exécution du job en millisecondes depuis son démarrage jusqu'à sa fin (ou, s'il n'est pas terminé, jusqu'à maintenant) ; <code>null</code> si le job n'est pas commencé
		 */
		public Long getDuration() {
			final Long duration;
			if (startTimestamp == null) {
				duration = null;
			}
			else {
				final long start = startTimestamp;
				final long end;
				if (endTimestamp == null) {
					end = System.nanoTime();
				}
				else {
					end = endTimestamp;
				}
				duration = (end - start) / NANOS_IN_MILLI;
			}
			return duration;
		}

		public String toString() {
			return String.format("{key=%s, extractor=%s, state=%s, result=%s}", key, extractor, state, result);
		}

		public String toDisplayString() {
			return String.format("Extraction '%s (%s)' demandée par %s en date du %s", extractor, key.getUuid(), key.getVisa(), DateHelper.dateTimeToDisplayString(creationDate));
		}

		@Override
		public String getRunningMessage() {
			return extractor.getRunningMessage();
		}

		@Override
		public Integer getPercentProgression() {
			return extractor.getPercentProgression();
		}

		@Override
		public String getDescription() {
			return extractor.getExtractionDescription();
		}
	}

	/**
	 * Classe de base pour lancer un travail d'extraction de manière polymorphique
	 */
	private abstract class ExtractorLauncher<T extends Extractor> {

		protected final T extractor;
		private final StatusManager statusManager;

		public ExtractorLauncher(T extractor) {
			Assert.notNull(extractor);
			this.extractor = extractor;
			this.statusManager = extractor.getStatusManager();
			if (this.statusManager != null) {
				this.statusManager.setMessage("En attente d'un créneau d'exécution...");
			}
		}

		public final ExtractionResult run() throws Exception {
			if (statusManager != null) {
				statusManager.setMessage("En cours...");
			}
			try {
				return doRun();
			}
			finally {
				if (statusManager != null) {
					statusManager.setMessage("Terminé.");
				}
			}
		}

		protected abstract ExtractionResult doRun() throws Exception;

		public final void interrupt() {
			extractor.interrupt();
		}

		public final String toString() {
			return extractor.toString();
		}

		public final String getExtractionName() {
			return extractor.getExtractionName();
		}

		public final String getRunningMessage() {
			return extractor.getRunningMessage();
		}

		public final Integer getPercentProgression() {
			return extractor.getPercentProgression();
		}

		public final String getExtractionDescription() {
			return extractor.getExtractionDescription();
		}
	}

	/**
	 * Launcher de base pour les extracteurs qui font tout eux-mêmes
	 */
	private final class PlainExtractorLauncher extends ExtractorLauncher<PlainExtractor> {

		public PlainExtractorLauncher(PlainExtractor extractor) {
			super(extractor);
		}

		@Override
		public ExtractionResult doRun() {
			return extractor.doExtraction();
		}
	}

	/**
	 * Launcher qui lance un extracteur de type {@link BatchableExtractor}
	 */
	private final class BatchableExtractorLauncher extends ExtractorLauncher<BatchableExtractor> {

		public BatchableExtractorLauncher(BatchableExtractor extractor) {
			super(extractor);
		}

		@SuppressWarnings({"unchecked"})
		@Override
		public ExtractionResult doRun() throws Exception {
			return runBatchExtractor(extractor);
		}
	}

	/**
	 * Launcher qui lance un extracteur de type {@link BatchableParallelExtractor}
	 */
	private final class BatchableParallelExtractorLauncher extends ExtractorLauncher<BatchableParallelExtractor> {

		public BatchableParallelExtractorLauncher(BatchableParallelExtractor extractor) {
			super(extractor);
		}

		@SuppressWarnings({"unchecked"})
		@Override
		public ExtractionResult doRun() throws Exception {
			return runParallelBatchExtractor(extractor);
		}
	}

	/**
	 * Classe d'exécution
	 */
	private final class Executor extends Thread {

		private boolean stopping = false;
		private ExtractionJobImpl currentJob = null;

		public Executor(int index) {
			super(String.format("Extraction-%d", index));
		}

		@Override
		public void run() {

			if (LOGGER.isInfoEnabled()) {
				LOGGER.info(String.format("Démarrage du thread d'extractions asynchrones %s", getName()));
			}

			AuthenticationHelper.pushPrincipal(getName());
			try {
				while (!stopping) {
					final ExtractionJobImpl job = queue.poll(1, TimeUnit.SECONDS);
					if (job != null && !stopping) {
						currentJob = job;
						ExtractionResult result = null;
						final ExtractorLauncher extractor = job.getExtractor();
						try {
							job.onStart();
							if (LOGGER.isInfoEnabled()) {
								LOGGER.info(String.format("Démarrage du job d'extraction %s (%s)", job.getKey(), extractor));
							}
							result = extractor.run();
						}
						catch (Throwable e) {
							LOGGER.error(String.format("Le job d'extraction %s a lancé une exception", job.getKey()), e);
							result = new ExtractionResultError(e);
						}
						finally {
							currentJob = null;
							job.onStop(result);
							nbJobsTermines.incrementAndGet();
						}

						if (LOGGER.isInfoEnabled()) {
							LOGGER.info(String.format("Arrêt du job d'extraction %s (%d ms) : %s", job.getKey(), job.getDuration(), result));
						}

						// à la fin du job, il faut envoyer ses résultats dans l'inbox du demandeur
						try {
							sendDocumentToInbox(job);
						}
						catch (Exception e) {
							LOGGER.error(String.format("Impossible d'envoyer le résultat de l'extraction %s dans l'inbox correpondante", job.getKey()), e);
						}
					}
				}
			}
			catch (InterruptedException e) {
				LOGGER.error("Thread d'extractions asynchrones arrêté sur une exception", e);
			}
			finally {
				if (LOGGER.isInfoEnabled()) {
					LOGGER.info(String.format("Arrêt du thread d'extractions asynchrones %s", getName()));
				}
				AuthenticationHelper.popPrincipal();
			}
		}

		/**
		 * Ask the executor to end its job
		 */
		public void end() {
			stopping = true;
			final ExtractionJobImpl job = currentJob;
			if (job != null) {
				job.getExtractor().interrupt();
			}
		}
	}

	private void sendDocumentToInbox(ExtractionJobImpl job) throws IOException {
		final String visa = job.key.getVisa();
		final UUID uuid = job.key.getUuid();
		final String docName = job.extractor.getExtractionName();
		final String descriptionBase = String.format("%s, commande du %s", job.getDescription(), DateHelper.dateTimeToDisplayString(job.getCreationDate()));
		final String description;
		InboxAttachment attachment = null;
		final ExtractionResult result = job.getResult();
		if (result != null) {
			description = String.format("%s, %s", descriptionBase, result);
			if (result instanceof ExtractionResultOk) {
				final ExtractionResultOk resultOk = (ExtractionResultOk) result;
				final InputStream in = resultOk.getStream();
				if (in != null) {
					final String mimeType = resultOk.getMimeType();
					final String filename = resultOk.getFilename();
					attachment = new InboxAttachment(mimeType, in, filename);
				}
			}
		}
		else {
			description = descriptionBase;
		}
		inboxService.addDocument(uuid, visa, docName, description, attachment, expiration * 24);
	}

	/**
	 * Interface pour pouvoir factoriser le code de lancement des extractions mono-threads et multi-threads
	 * @param <E> classe des éléments en entrée du découpage en lots
	 * @param <R> classe de résultat de chacun des lots
	 * @param <T> classe de l'extracteur utilisé
	 */
	private static interface CustomBatchableRun<E, R extends BatchResults<E, R>, T extends BatchableExtractor<E, R>> {
		void run(T extractor, R rapportFinal, List<E> elements);
	}

	/**
	 * Lancement d'un travail d'extraction par lots
	 * @param extractor l'extracteur
	 * @param action l'action spécifique pour le lancement de l'extracteur
	 * @param <E> classe des éléments en entrée du découpage en lots
	 * @param <R> classe de résultat de chacun des lots
	 * @param <T> classe de l'extracteur utilisé
	 * @return le résultat de l'extraction
	 * @throws IOException en cas de problème I/O
	 */
	private <E, R extends BatchResults<E, R>, T extends BatchableExtractor<E, R>> ExtractionResult runBatchableExtractor(T extractor, CustomBatchableRun<E, R, T> action) throws IOException {
		final R rapportFinal = extractor.createRapport(true);
		final List<E> elements = getElements(extractor);
		action.run(extractor, rapportFinal, elements);
		final InputStream stream = extractor.getStreamForExtraction(rapportFinal);
		final String mimeType = extractor.getMimeType();
		final String filename = extractor.getFilename();
		return new ExtractionResultOk(stream, mimeType, filename, extractor.isInterrupted());
	}

	/**
	 * Lancement d'un travail d'extraction par lots multi-threads
	 * @param extractor extracteur
	 * @param <E> classe des éléments en entrée du découpage en lots
	 * @param <R> classe de résultat de chacun des lots
	 * @return le résultat de l'extraction
	 * @throws IOException en cas de problème I/O
	 */
	private <E, R extends BatchResults<E, R>> ExtractionResult runParallelBatchExtractor(BatchableParallelExtractor<E, R> extractor) throws IOException {
		return runBatchableExtractor(extractor, new CustomBatchableRun<E, R, BatchableParallelExtractor<E, R>>() {
			@Override
			public void run(BatchableParallelExtractor<E, R> extractor, R rapportFinal, List<E> elements) {
				final ParallelBatchTransactionTemplate<E, R> batch = new ParallelBatchTransactionTemplate<E, R>(elements, extractor.getBatchSize(), extractor.getNbThreads(), extractor.getBatchBehavior(), transactionManager, extractor.getStatusManager(), hibernateTemplate);
				batch.setReadonly(true);        // ce sont toutes des extractions !
				batch.execute(rapportFinal, createCallback(extractor, rapportFinal));
			}
		});
	}

	/**
	 * Lancement d'un travail d'extraction par lots mono-threads
	 * @param extractor extracteur
	 * @param <E> classe des éléments en entrée du découpage en lots
	 * @param <R> classe de résultat de chacun des lots
	 * @return le résultat de l'extraction
	 * @throws IOException en cas de problème I/O
	 */
	private <E, R extends BatchResults<E, R>> ExtractionResult runBatchExtractor(BatchableExtractor<E, R> extractor) throws IOException {
		return runBatchableExtractor(extractor, new CustomBatchableRun<E, R, BatchableExtractor<E, R>>() {
			@Override
			public void run(BatchableExtractor<E, R> extractor, R rapportFinal, List<E> elements) {
				final BatchTransactionTemplate<E, R> batch = new BatchTransactionTemplate<E, R>(elements, extractor.getBatchSize(), extractor.getBatchBehavior(), transactionManager, extractor.getStatusManager(), hibernateTemplate);
				batch.setReadonly(true);        // ce sont toutes des extractions !
				batch.execute(rapportFinal, createCallback(extractor, rapportFinal));
			}
		});
	}

	/**
	 * @param extractor extracteur
	 * @param rapportFinal le rapport final de l'extraction
	 * @param <E> classe des éléments en entrée du découpage en lots
	 * @param <R> classe de résultat de chacun des lots
	 * @return le code qui sera exécuté pour chaque lot
	 */
	private static <E, R extends BatchResults<E, R>> BatchTransactionTemplate.BatchCallback<E, R> createCallback(final BatchableExtractor<E, R> extractor, final R rapportFinal) {
		return new BatchTransactionTemplate.BatchCallback<E, R>() {
			@Override
			public boolean doInTransaction(List<E> batch, R rapport) throws Exception {
				return !extractor.isInterrupted() && extractor.doBatchExtraction(batch, rapport);
			}

			@Override
			public R createSubRapport() {
				return extractor.createRapport(false);
			}

			@Override
			public void afterTransactionCommit() {
				super.afterTransactionCommit();
				extractor.afterTransactionCommit(rapportFinal, percent);
			}
		};
	}

	/**
	 * Construit une transaction read-only dans laquelle la méthode {@link BatchableExtractor#buildElementList()} est appelée
	 * @param extractor extracteur auquel il faut demander sa liste d'éléments
	 * @return la liste renvoyée par {@link BatchableExtractor#buildElementList()}
	 */
	@SuppressWarnings({"unchecked"})
	private <E, R extends BatchResults<E, R>> List<E> getElements(final BatchableExtractor<E, R> extractor) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return (List<E>) template.execute(new TransactionCallback() {
			@Override
			public List<E> doInTransaction(TransactionStatus status) {
				return extractor.buildElementList();
			}
		});
	}

	@Override
	public ExtractionKey postExtractionQuery(String visa, PlainExtractor extractor) {
		return postExtractionQuery(visa, new PlainExtractorLauncher(extractor));
	}

	@Override
	public ExtractionKey postExtractionQuery(String visa, BatchableExtractor extractor) {
		return postExtractionQuery(visa, new BatchableExtractorLauncher(extractor));
	}

	@Override
	public ExtractionKey postExtractionQuery(String visa, BatchableParallelExtractor extractor) {
		return postExtractionQuery(visa, new BatchableParallelExtractorLauncher(extractor));
	}

	private ExtractionKey postExtractionQuery(String visa, ExtractorLauncher launcher) {
		Assert.isTrue(StringUtils.isNotBlank(visa), "Le visa ne doit pas être vide");

		// tout d'abord, on génère une clé pour cette extraction
		final ExtractionKey key = new ExtractionKey(visa);

		// puis on poste la demande d'exécution dans la queue
		final ExtractionJobImpl jobInfo = new ExtractionJobImpl(key, launcher);
		queue.add(jobInfo);
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Job d'extraction %s enregistré (%s)", key, launcher));
		}

		// et enfin on retourne la clé
		return key;
	}

	@Override
	public void destroy() throws Exception {

		// on demande l'arrêt de tous les exécuteurs
		for (Executor exec : executors) {
			exec.end();
		}

		// on attend l'arrêt de tous les exécuteurs
		for (Executor exec : executors) {
			exec.join();
		}

		LOGGER.info("Service d'extractions asychrones arrêté");
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		Assert.isTrue(expiration > 0, "La valeur en jours de la durée de validité des extractions doit être strictement positive");
		Assert.isTrue(threadPoolSize > 0, "Le nombre de threads dans le pool doit être strictement positif");

		// on construit les exécuteurs
		executors = new ArrayList<Executor>(threadPoolSize);
		for (int i = 0 ; i < threadPoolSize ; ++ i) {
			executors.add(new Executor(i));
		}

		// puis on les lance
		for (Executor exec : executors) {
			exec.start();
		}

		LOGGER.info(String.format("Service d'extractions asychrones démarré avec %d exécuteur(s)", threadPoolSize));
	}

	@Override
	public int getDelaiExpirationEnJours() {
		return expiration;
	}

	@Override
	public int getNbExecutors() {
		return threadPoolSize;
	}

	@Override
	public List<ExtractionJob> getQueueContent(String visa) {
		final List<ExtractionJob> liste = new ArrayList<ExtractionJob>(queue);
		if (visa != null) {
			final Iterator<ExtractionJob> iterator = liste.iterator();
			while (iterator.hasNext()) {
				final ExtractionJob job = iterator.next();
				if (!visa.equals(job.getKey().getVisa())) {
					iterator.remove();
				}
			}
		}
		return liste;
	}

	@Override
	public int getQueueSize() {
		return queue.size();
	}

	@Override
	public int getNbExecutedQueries() {
		return nbJobsTermines.get();
	}

	@Override
	public List<ExtractionJob> getExtractionsEnCours(String visa) {
		final List<ExtractionJob> liste = new ArrayList<ExtractionJob>(threadPoolSize);
		for (Executor executor : executors) {
			final ExtractionJob enCours = executor.currentJob;
			if (enCours != null) {
				if (visa == null || visa.equals(enCours.getKey().getVisa())) {
					liste.add(enCours);
				}
			}
		}
		return liste;
	}
}
