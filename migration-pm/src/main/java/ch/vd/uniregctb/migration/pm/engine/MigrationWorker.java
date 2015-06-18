package ch.vd.uniregctb.migration.pm.engine;

import java.util.Arrays;
import java.util.List;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.DefaultThreadFactory;
import ch.vd.uniregctb.common.DefaultThreadNameGenerator;
import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.MigrationMode;
import ch.vd.uniregctb.migration.pm.MigrationResultMessage;
import ch.vd.uniregctb.migration.pm.MigrationResultMessageProvider;
import ch.vd.uniregctb.migration.pm.Worker;
import ch.vd.uniregctb.migration.pm.utils.EntityMigrationSynchronizer;

public class MigrationWorker implements Worker, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationWorker.class);

	private static final RejectedExecutionHandler ABORT_POLICY = new ThreadPoolExecutor.AbortPolicy();

	private final AtomicInteger nbEnCours = new AtomicInteger(0);
	private final EntityMigrationSynchronizer synchronizer = new EntityMigrationSynchronizer();
	private ExecutorService executor;
	private CompletionService<MigrationResultMessageProvider> completionService;
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

	private static void log(Logger logger, MigrationResultMessage.Niveau niveau, String msg) {
		final Consumer<String> realLogger;
		switch (niveau) {
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
			throw new IllegalArgumentException("Niveau invalide : " + niveau);
		}

		// envoi dans le log
		realLogger.accept(msg);
	}

	private static String dump(Throwable t) {
		return t.getClass().getName() + ": " + t.getMessage() + ExceptionUtils.getStackTrace(t);
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
		this.executor = new ThreadPoolExecutor(nbThreads, nbThreads, 0L, TimeUnit.SECONDS,
		                                       new ArrayBlockingQueue<>(20),
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
				while (!(stopOnNextLoop || (stopOnExhaustion && nbEnCours.get() == 0))) {
					final Future<MigrationResultMessageProvider> future = completionService.poll(1, TimeUnit.SECONDS);
					if (future != null) {
						nbEnCours.decrementAndGet();

						try {
							final MigrationResultMessageProvider res = future.get();
							if (LOGGER.isDebugEnabled()) {
								LOGGER.debug("Résultat de migration reçu : " + res);
							}

							// utilisation des loggers pour les fichiers/listes de contrôle
							for (MigrationResultMessage.CategorieListe cat : MigrationResultMessage.CategorieListe.values()) {
								final List<MigrationResultMessage> messages = res.getMessages(cat);
								if (!messages.isEmpty()) {
									final Logger logger = LoggerFactory.getLogger(String.format("%s.%s", MigrationResultMessage.CategorieListe.class.getName(), cat.name()));
									messages.forEach(msg -> log(logger, msg.getNiveau(), msg.getTexte()));
								}
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
	private MigrationResultMessageProvider migrateGraphe(Graphe graphe) {

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

			final MigrationResult res = new MigrationResult();
			final String msg = String.format("Les entreprises %s n'ont pas pu être migrées : %s", Arrays.toString(idsEntreprise.toArray(new Long[idsEntreprise.size()])), dump(t));
			res.addMessage(MigrationResultMessage.CategorieListe.GENERIQUE, MigrationResultMessage.Niveau.ERROR, msg);
			return res;
		}
	}
}
