package ch.vd.unireg.extraction;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import ch.vd.registre.base.date.DateHelper;
import ch.vd.shared.batchtemplate.BatchResults;
import ch.vd.shared.batchtemplate.BatchWithResultsCallback;
import ch.vd.shared.batchtemplate.SimpleProgressMonitor;
import ch.vd.unireg.common.AuthenticationHelper;
import ch.vd.unireg.common.AuthenticationInterface;
import ch.vd.unireg.common.BatchTransactionTemplateWithResults;
import ch.vd.unireg.common.DefaultThreadNameGenerator;
import ch.vd.unireg.common.MonitorableExecutorService;
import ch.vd.unireg.common.ParallelBatchTransactionTemplateWithResults;
import ch.vd.unireg.common.StatusManager;
import ch.vd.unireg.common.TemporaryFile;
import ch.vd.unireg.common.ThreadNameGenerator;
import ch.vd.unireg.inbox.InboxAttachment;
import ch.vd.unireg.inbox.InboxService;

/**
 * Implémentation du service d'extractions asynchrones
 */
public class ExtractionServiceImpl implements ExtractionService, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(ExtractionServiceImpl.class);

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
	 * Service de gestion des documents une fois l'extraction terminée
	 */
	private InboxService inboxService;

	/**
	 * Pool de thread d'exécution
	 */
	private MonitorableExecutorService<ExtractionResult, ExtractionJobImpl> executorService;

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
	public void setInboxService(InboxService inboxService) {
		this.inboxService = inboxService;
	}

	private enum JobState {
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
	private final class ExtractionJobImpl implements ExtractionJob, Callable<ExtractionResult> {

		private final UUID uuid;
		private final String visa;
		private final ExtractorLauncher extractor;
		private final Date creationDate;
		private Long startTimestamp;
		private Long endTimestamp;
		private JobState state;
		private ExtractionResult result;

		public ExtractionJobImpl(String visa, ExtractorLauncher extractor) {
			this.uuid = UUID.randomUUID();
			this.visa = visa;
			this.extractor = extractor;
			this.state = JobState.WAITING_FOR_START;
			this.startTimestamp = null;
			this.endTimestamp = null;
			this.creationDate = DateHelper.getCurrentDate();
		}

		@Override
		public ExtractionResult getResult() {
			return result;
		}

		public void onStart() {
			if (this.state != JobState.WAITING_FOR_START) {
				throw new IllegalArgumentException();
			}
			this.state = JobState.RUNNING;
			this.startTimestamp = System.nanoTime();
		}

		public void onStop(ExtractionResult result) {
			if (this.state == JobState.STOPPED || this.state == JobState.CANCELLED) {
				throw new IllegalStateException();
			}
			if (this.state == JobState.RUNNING) {
				this.state = JobState.STOPPED;
			}
			else if (this.state == JobState.WAITING_FOR_START) {
				this.state = JobState.CANCELLED;
			}
			this.result = result;
			this.endTimestamp = System.nanoTime();
		}

		@Override
		public UUID getUuid() {
			return uuid;
		}

		@Override
		public String getVisa() {
			return visa;
		}

		@Override
		public boolean isRunning() {
			return state == JobState.RUNNING;
		}

		@Override
		public boolean isInterrupted() {
			return extractor.wasInterrupted();
		}

		@Override
		public Date getCreationDate() {
			return creationDate;
		}

		/**
		 * @return la durée d'exécution du job en millisecondes depuis son démarrage jusqu'à sa fin (ou, s'il n'est pas terminé, jusqu'à maintenant) ; <code>null</code> si le job n'est pas commencé
		 */
		@Override
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
			return String.format("{uuid=%s, visa=%s, extractor=%s, creation-date=%s, state=%s, running-msg=%s, progress=%d, result=%s}",
			                     uuid, visa, extractor, creationDate, state, getRunningMessage(), getPercentProgression(), result);
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

		@Override
		public void interrupt() {
			extractor.interrupt();
		}

		@Override
		public int hashCode() {
			return uuid.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return obj instanceof ExtractionJobImpl && uuid.equals(((ExtractionJobImpl) obj).uuid);
		}

		@Override
		public ExtractionResult call() throws Exception {

			AuthenticationHelper.pushPrincipal(visa);
			try {
				ExtractionResult result = null;
				try {
					onStart();
					if (LOGGER.isInfoEnabled()) {
						LOGGER.info(String.format("Démarrage du job d'extraction %s (%s)", uuid, extractor));
					}
					result = extractor.call();
				}
				catch (Throwable e) {
					LOGGER.error(String.format("Le job d'extraction %s a lancé une exception", uuid), e);
					result = new ExtractionResultError(e);
				}
				finally {
					onStop(result);
					nbJobsTermines.incrementAndGet();
				}

				if (LOGGER.isInfoEnabled()) {
					LOGGER.info(String.format("Arrêt du job d'extraction %s (%d ms) : %s", uuid, getDuration(), result));
				}

				// à la fin du job, il faut envoyer ses résultats dans l'inbox du demandeur
				try {
					sendDocumentToInbox(this);
				}
				catch (Throwable e) {
					LOGGER.error(String.format("Impossible d'envoyer le résultat de l'extraction %s dans l'inbox correpondante", uuid), e);
				}

				return result;
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	/**
	 * Classe de base pour lancer un travail d'extraction de manière polymorphique
	 */
	private abstract class ExtractorLauncher<T extends Extractor> implements Callable<ExtractionResult> {

		protected final T extractor;
		private final StatusManager statusManager;

		public ExtractorLauncher(T extractor) {
			if (extractor == null) {
				throw new IllegalArgumentException();
			}
			this.extractor = extractor;
			this.statusManager = extractor.getStatusManager();
			if (this.statusManager != null) {
				this.statusManager.setMessage("En attente d'un créneau d'exécution...");
			}
		}

		@Override
		public final ExtractionResult call() throws Exception {
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

		public final boolean wasInterrupted() {
			return extractor.wasInterrupted();
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
		public ExtractionResult doRun() throws Exception {
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

	private void sendDocumentToInbox(ExtractionJobImpl job) throws IOException {
		final String visa = job.getVisa();
		final UUID uuid = job.getUuid();
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
					final String filenameRadical = resultOk.getFilenameRadical();
					attachment = new InboxAttachment(mimeType, in, filenameRadical);
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
	private interface CustomBatchableRun<E, R extends BatchResults<E, R>, T extends BatchableExtractor<E, R>> {
		void run(T extractor, R rapportFinal, List<E> elements);
	}

	/**
	 * InputStream basé sur un fichier temporaire qui doit être effacé au moment de la clôture du flux
	 */
	private static class TemporaryFileInputStream extends InputStream {
		private final TemporaryFile file;
		private final InputStream is;

		private TemporaryFileInputStream(TemporaryFile file) throws IOException {
			this.file = file;
			this.is = file.openInputStream();
		}

		@Override
		public int read() throws IOException {
			return is.read();
		}

		@Override
		public int read(@NotNull byte[] b) throws IOException {
			return is.read(b);
		}

		@Override
		public int read(@NotNull byte[] b, int off, int len) throws IOException {
			return is.read(b, off, len);
		}

		@Override
		public long skip(long n) throws IOException {
			return is.skip(n);
		}

		@Override
		public int available() throws IOException {
			return is.available();
		}

		@Override
		public void close() throws IOException {
			is.close();
			file.close();
		}

		@Override
		public void mark(int readlimit) {
			is.mark(readlimit);
		}

		@Override
		public void reset() throws IOException {
			is.reset();
		}

		@Override
		public boolean markSupported() {
			return is.markSupported();
		}
	}

	/**
	 * Flux toujours vide
	 */
	private static class EmptyInputStream extends InputStream {
		@Override
		public int read() throws IOException {
			return -1;
		}
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

		final String mimeType = extractor.getMimeType();
		final String filenameRadical = extractor.getFilenameRadical();

		// Attention : c'est seulement en cas d'exception remontée qu'il faut fermer les flux !
		// (en effet, et c'est pour cela que l'on ne peut pas utiliser le try-with-resource, la lecture
		// des données dans le flux "is" est faite beaucoup plus tard...)

		final TemporaryFile contentFile = extractor.getExtractionContent(rapportFinal);
		if (contentFile != null) {
			try {
				final InputStream is = new TemporaryFileInputStream(contentFile);
				try {
					return new ExtractionResultOk(is, mimeType, filenameRadical, extractor.wasInterrupted());
				}
				catch (RuntimeException | Error e) {
					is.close();
					throw e;
				}
			}
			catch (RuntimeException | Error | IOException e) {
				contentFile.close();
				throw e;
			}
		}
		else {
			return new ExtractionResultOk(new EmptyInputStream(), mimeType, filenameRadical, extractor.wasInterrupted());
		}
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
				final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
				final ParallelBatchTransactionTemplateWithResults<E, R>
						batch = new ParallelBatchTransactionTemplateWithResults<>(elements, extractor.getBatchSize(), extractor.getNbThreads(), extractor.getBatchBehavior(), transactionManager,
						                                                          extractor.getStatusManager(), AuthenticationInterface.INSTANCE);
				batch.setReadonly(true);        // ce sont toutes des extractions !
				batch.execute(rapportFinal, createCallback(extractor, rapportFinal, progressMonitor), progressMonitor);
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
				final SimpleProgressMonitor progressMonitor = new SimpleProgressMonitor();
				final BatchTransactionTemplateWithResults<E, R>
						batch = new BatchTransactionTemplateWithResults<>(elements, extractor.getBatchSize(), extractor.getBatchBehavior(), transactionManager, extractor.getStatusManager());
				batch.setReadonly(true);        // ce sont toutes des extractions !
				batch.execute(rapportFinal, createCallback(extractor, rapportFinal, progressMonitor), progressMonitor);
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
	private static <E, R extends BatchResults<E, R>> BatchWithResultsCallback<E, R> createCallback(final BatchableExtractor<E, R> extractor, final R rapportFinal, final SimpleProgressMonitor progressMonitor) {
		return new BatchWithResultsCallback<E, R>() {
			@Override
			public boolean doInTransaction(List<E> batch, R rapport) throws Exception {
				return !extractor.wasInterrupted() && extractor.doBatchExtraction(batch, rapport);
			}

			@Override
			public R createSubRapport() {
				return extractor.createRapport(false);
			}

			@Override
			public void afterTransactionCommit() {
				super.afterTransactionCommit();
				extractor.afterTransactionCommit(rapportFinal, progressMonitor.getProgressInPercent());
			}
		};
	}

	/**
	 * Construit une transaction read-only dans laquelle la méthode {@link BatchableExtractor#buildElementList()} est appelée
	 * @param extractor extracteur auquel il faut demander sa liste d'éléments
	 * @return la liste renvoyée par {@link BatchableExtractor#buildElementList()}
	 */
	private <E, R extends BatchResults<E, R>> List<E> getElements(final BatchableExtractor<E, R> extractor) {
		final TransactionTemplate template = new TransactionTemplate(transactionManager);
		template.setReadOnly(true);

		return template.execute(status -> extractor.buildElementList());
	}

	@Override
	public ExtractionJob postExtractionQuery(String visa, PlainExtractor extractor) {
		return postExtractionQuery(visa, new PlainExtractorLauncher(extractor));
	}

	@Override
	public ExtractionJob postExtractionQuery(String visa, BatchableExtractor extractor) {
		return postExtractionQuery(visa, new BatchableExtractorLauncher(extractor));
	}

	@Override
	public ExtractionJob postExtractionQuery(String visa, BatchableParallelExtractor extractor) {
		return postExtractionQuery(visa, new BatchableParallelExtractorLauncher(extractor));
	}

	@SuppressWarnings({"SuspiciousMethodCalls"})
	@Override
	public void cancelJob(ExtractionJob job) {
		job.interrupt();                                      // s'il est en cours, il s'arrêtera tout seul
		executorService.cancel((ExtractionJobImpl) job);      // s'il n'est pas encore en cours, il sera éliminé de la queue
	}

	private ExtractionJob postExtractionQuery(String visa, ExtractorLauncher launcher) {

		if (StringUtils.isBlank(visa)) {
			throw new IllegalArgumentException("Le visa ne doit pas être vide");
		}

		// puis on poste la demande d'exécution dans la queue
		final ExtractionJobImpl jobInfo = new ExtractionJobImpl(visa, launcher);
		executorService.submit(jobInfo);
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info(String.format("Job d'extraction %s enregistré pour le visa %s (%s)", jobInfo.getUuid(), visa, launcher));
		}

		// et enfin on retourne le job
		return jobInfo;
	}

	@Override
	public void destroy() throws Exception {

		// on demande l'arrêt de tous les exécuteurs
		executorService.shutdown();
		for (ExtractionJob job : executorService.getRunning()) {
			job.interrupt();
		}
		while (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {}

		LOGGER.info("Service d'extractions asychrones arrêté");
	}

	/**
	 * Thread d'extraction
	 */
	private static final class ExtractorThread extends Thread {

		private ExtractorThread(Runnable target, String name) {
			super(target, name);
		}

		@Override
		public void run() {
			LOGGER.info("Démarrage du thread " + getName());
			try {
				super.run();
			}
			finally {
				LOGGER.info("Arrêt du thread " + getName());
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception {

		if (expiration <= 0) {
			throw new IllegalArgumentException("La valeur en jours de la durée de validité des extractions doit être strictement positive");
		}
		if (threadPoolSize <= 0) {
			throw new IllegalArgumentException("Le nombre de threads dans le pool doit être strictement positif");
		}

		final ThreadNameGenerator threadNameGenerator = new DefaultThreadNameGenerator("Extraction");
		executorService = new MonitorableExecutorService<>(Executors.newFixedThreadPool(threadPoolSize, r -> new ExtractorThread(r, threadNameGenerator.getNewThreadName())));

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
		return new ArrayList<>(executorService.getWaiting());
	}

	@Override
	public int getQueueSize() {
		return executorService.getWaitingSize();
	}

	@Override
	public int getNbExecutedQueries() {
		return nbJobsTermines.get();
	}

	@Override
	public List<ExtractionJob> getExtractionsEnCours(String visa) {
		final Collection<ExtractionJobImpl> running = executorService.getRunning();
		final List<ExtractionJob> liste = new ArrayList<>(running.size());
		for (ExtractionJob job : running) {
			if (visa == null || visa.equals(job.getVisa())) {
				liste.add(job);
			}
		}
		return liste;
	}
}
