package ch.vd.uniregctb.migration.pm.engine;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.DefaultThreadFactory;
import ch.vd.uniregctb.common.DefaultThreadNameGenerator;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationMode;
import ch.vd.uniregctb.migration.pm.Worker;
import ch.vd.uniregctb.migration.pm.log.LogCategory;
import ch.vd.uniregctb.migration.pm.log.LogLevel;
import ch.vd.uniregctb.migration.pm.log.LoggedElementAttribute;
import ch.vd.uniregctb.migration.pm.log.LoggedElementRenderer;
import ch.vd.uniregctb.migration.pm.log.LoggedMessage;
import ch.vd.uniregctb.migration.pm.log.LoggedMessages;
import ch.vd.uniregctb.migration.pm.log.MessageLoggedElement;
import ch.vd.uniregctb.migration.pm.utils.EntityMigrationSynchronizer;

public class MigrationWorker implements Worker, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationWorker.class);

	private static final RejectedExecutionHandler ABORT_POLICY = new ThreadPoolExecutor.AbortPolicy();

	private final AtomicInteger nbEnCours = new AtomicInteger(0);
	private final EntityMigrationSynchronizer synchronizer = new EntityMigrationSynchronizer();
	private BlockingQueue<Runnable> queue;
	private ExecutorService executor;
	private CompletionService<LoggedMessages> completionService;
	private GatheringThread gatheringThread;

	private MigrationMode mode;
	private GrapheMigrator grapheMigrator;
	private int nbThreads = 1;

	/**
	 * Appelé quand une tâche ne peut être ajoutée à la queue d'entrée d'un {@link ThreadPoolExecutor}, afin d'attendre patiemment
	 * qu'une place se libère
	 * @param r action à placer sur la queue
	 * @param executor exécuteur qui a refusé la tâche
	 */
	private static void rejectionExecutionHandler(Runnable r, ThreadPoolExecutor executor) {
		final BlockingQueue<Runnable> queue = executor.getQueue();
		boolean sent = false;
		try {
			while (!sent) {
				if (executor.isShutdown()) {
					// this will trigger the abort policy
					break;
				}

				// if sent successfully, that's the key out of the loop!
				sent = queue.offer(r, 1, TimeUnit.SECONDS);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}

		if (!sent) {
			ABORT_POLICY.rejectedExecution(r, executor);
		}
	}

	@NotNull
	private static LogLevel extractLevel(LoggedMessage elt, @NotNull LogLevel defaultLevel) {
		return elt.getLevel() == null ? defaultLevel : elt.getLevel();
	}

	private static void log(Logger logger, LoggedMessage elt) {
		final LogLevel level = extractLevel(elt, LogLevel.INFO);
		final Consumer<String> realLogger;
		switch (level) {
		case DEBUG:
			realLogger = logger::debug;
			break;
		case ERROR:
			realLogger = logger::error;
			break;
		case INFO:
			realLogger = logger::info;
			break;
		case WARN:
			realLogger = logger::warn;
			break;
		default:
			throw new IllegalArgumentException("Niveau invalide : " + level);
		}

		// envoi dans le log
		realLogger.accept(elt.getMessage());
	}

	/**
	 * Dump la stack de l'exception passée en paramètre dans une chaîne de caractères
	 * @param t l'exception utilisée
	 * @return la chaîne de caractères contenant la stack
	 */
	static String dump(Throwable t) {
		// pas la peine d'ajouter manuellement le t.getMessage() car il est déjà pris en compte dans ExceptionUtils.getStackTrace()
		return t.getClass().getName() + ": " + ExceptionUtils.getStackTrace(t);
	}

	public void setMode(MigrationMode mode) {
		this.mode = mode;
	}

	public void setGrapheMigrator(GrapheMigrator grapheMigrator) {
		this.grapheMigrator = grapheMigrator;
	}

	public void setNbThreads(int nbThreads) {
		this.nbThreads = nbThreads;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.queue = new ArrayBlockingQueue<>(20);
		this.executor = new ThreadPoolExecutor(nbThreads, nbThreads, 0L, TimeUnit.SECONDS,
		                                       queue,
		                                       new DefaultThreadFactory(new DefaultThreadNameGenerator("Migrator")),
		                                       MigrationWorker::rejectionExecutionHandler);

		this.completionService = new ExecutorCompletionService<>(this.executor);
		this.gatheringThread = new GatheringThread();
		if (this.mode == MigrationMode.FROM_DUMP || this.mode == MigrationMode.DIRECT) {
			this.gatheringThread.start();
		}
	}

	@Override
	public void destroy() throws Exception {
		executor.shutdownNow();
		awaitExecutorTermination();
		gatheringThread.requestStop();
		gatheringThread.join();
	}

	/**
	 * Thread de récupération (et de log) des résultats de migration
	 */
	private final class GatheringThread extends Thread {

		private volatile boolean stopOnNextLoop = false;
		private volatile boolean stopOnExhaustion = false;

		public GatheringThread() {
			super("Gathering");
		}

		@Override
		public void run() {
			LOGGER.info("Gathering thread starting...");
			try {

				// dump des noms de colonnes dans les logs "type CSV"
				for (LogCategory cat : LogCategory.values()) {
					final Logger logger = getLogger(cat);
					final List<LoggedElementAttribute> columns = MigrationResult.getColumns(cat);
					if (!columns.isEmpty()) {
						final String line = LoggedElementRenderer.renderColumns(columns);
						logger.info(line);
					}
				}

				// boucle principale du thread... réception des résultats de migration de graphes
				while (!(stopOnNextLoop || (stopOnExhaustion && nbEnCours.get() == 0))) {
					final Future<LoggedMessages> future = completionService.poll(1, TimeUnit.SECONDS);
					if (future != null) {
						nbEnCours.decrementAndGet();

						try {
							final LoggedMessages res = future.get();

							// utilisation des loggers pour les fichiers/listes de contrôle
							for (Map.Entry<LogCategory, List<LoggedMessage>> entry : res.asMap().entrySet()) {
								final Logger logger = getLogger(entry.getKey());
								entry.getValue().forEach(msg -> log(logger, msg));
							}
						}
						catch (ExecutionException e) {
							LOGGER.error("Exception inattendue", e.getCause());
						}
					}
				}
			}
			catch (InterruptedException e) {
				LOGGER.error("Gathering thread interrupted!", e);
			}
			catch (RuntimeException | Error e) {
				LOGGER.error("Exception thrown in gathering thread", e);
			}
			finally {
				LOGGER.info("Gathering thread is now done.");
			}
		}

		public void requestStop() {
			this.stopOnNextLoop = true;
		}

		public void signalFeedingOver() {
			this.stopOnExhaustion = true;
		}
	}

	/**
	 * @param category une catégorie de log
	 * @return le logger responsable de cette catégorie
	 */
	@NotNull
	private static Logger getLogger(LogCategory category) {
		return LoggerFactory.getLogger(String.format("%s.%s", LogCategory.class.getName(), category.name()));
	}

	@Override
	public void onGraphe(Graphe graphe) throws Exception {
		completionService.submit(() -> migrateGraphe(graphe));
		nbEnCours.incrementAndGet();
	}

	@Override
	public void feedingOver() throws InterruptedException {
		executor.shutdown();
		awaitExecutorTermination();
		gatheringThread.signalFeedingOver();
	}

	private void awaitExecutorTermination() throws InterruptedException {
		while (!executor.isTerminated()) {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		}
	}

	/**
	 * Appelé de manière asynchrone pour migrer un graphe d'entité
	 * @param graphe le graphe à migrer
	 * @return les indications issues de la migration
	 */
	private LoggedMessages migrateGraphe(Graphe graphe) {

		LOGGER.info(String.format("Migration du graphe %s", graphe));

		final Set<Long> idsEntreprise = graphe.getEntreprises().keySet();
		final Set<Long> idsIndividus = graphe.getIndividus().keySet();
		try {
			while (true) {
				final EntityMigrationSynchronizer.Ticket ticket = synchronizer.hold(idsEntreprise, idsIndividus, 1000);
				if (ticket != null) {
					try {
						return grapheMigrator.migrate(graphe);
					}
					finally {
						synchronizer.release(ticket);
					}
				}
				else {
					LOGGER.info(String.format("L'une des entreprises %s ou des individus %s est déjà en cours de migration en ce moment même... On attend...",
					                          Arrays.toString(idsEntreprise.toArray(new Long[idsEntreprise.size()])),
					                          Arrays.toString(idsIndividus.toArray(new Long[idsIndividus.size()]))));
				}
			}
		}
		catch (Throwable t) {
			if (t instanceof InterruptedException) {
				Thread.currentThread().interrupt();
			}

			final MessageLoggedElement elt = new MessageLoggedElement(LogLevel.ERROR,
			                                                          String.format("Les entreprises %s n'ont pas pu être migrées : %s",
			                                                                        Arrays.toString(idsEntreprise.toArray(new Long[idsEntreprise.size()])),
			                                                                        dump(t)));
			return LoggedMessages.singleton(LogCategory.EXCEPTIONS, elt.resolve());
		}
	}

	/**
	 * @return le nombre de graphe actuellement en attente de traitement dans la file d'attente
	 */
	public int getTailleFileAttente() {
		return queue != null ? queue.size() : 0;
	}

	/**
	 * @return le nombre de graphes actuellement en cours de traitement
	 */
	public int getNombreMigrationsEnCours() {
		return Math.max(0, nbEnCours.intValue() - getTailleFileAttente());
	}
}
