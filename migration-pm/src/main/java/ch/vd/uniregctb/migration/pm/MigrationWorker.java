package ch.vd.uniregctb.migration.pm;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.transaction.PlatformTransactionManager;

import ch.vd.uniregctb.common.DefaultThreadFactory;
import ch.vd.uniregctb.common.DefaultThreadNameGenerator;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;
import ch.vd.uniregctb.migration.pm.utils.EntityMigrationSynchronizer;

public class MigrationWorker implements Worker, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationWorker.class);

	private static final RejectedExecutionHandler ABORT_POLICY = new ThreadPoolExecutor.AbortPolicy();

	private ExecutorService executor;
	private CompletionService<MigrationResult> completionService;
	private Thread gatheringThread;
	private final AtomicInteger nbEnCours = new AtomicInteger(0);
	private final EntityMigrationSynchronizer synchronizer = new EntityMigrationSynchronizer();
	private volatile boolean started;

	private StreetDataMigrator streetDataMigrator;
	private PlatformTransactionManager uniregTransactionManager;

	public void setStreetDataMigrator(StreetDataMigrator streetDataMigrator) {
		this.streetDataMigrator = streetDataMigrator;
	}

	public void setUniregTransactionManager(PlatformTransactionManager uniregTransactionManager) {
		this.uniregTransactionManager = uniregTransactionManager;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
		                                       new ArrayBlockingQueue<>(20),
		                                       new DefaultThreadFactory(new DefaultThreadNameGenerator("Migrator")),
		                                       (r, executor) -> {
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
		                                       });

		this.completionService = new ExecutorCompletionService<>(this.executor);
		this.gatheringThread = new Thread("Gathering") {
			@Override
			public void run() {
				LOGGER.info("Gathering thread starting...");
				try {
					gather();
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
		};
		this.gatheringThread.start();
	}

	@Override
	public void destroy() throws Exception {
		final List<Runnable> remaining = executor.shutdownNow();
		nbEnCours.addAndGet(-remaining.size());
		awaitTermination();
		gatheringThread.join();
	}

	@Override
	public void onGraphe(Graphe graphe) throws Exception {
		completionService.submit(new MigrationTask(graphe));
		nbEnCours.incrementAndGet();
		started = true;
	}

	@Override
	public void feedingOver() throws InterruptedException {
		executor.shutdown();
		awaitTermination();
	}

	private void awaitTermination() throws InterruptedException {
		while (!executor.isTerminated()) {
			executor.awaitTermination(1, TimeUnit.SECONDS);
		}
	}

	private void gather() throws InterruptedException {
		while ((!started && !executor.isTerminated()) || (started && nbEnCours.intValue() > 0)) {
			final Future<MigrationResult> future = completionService.poll(1, TimeUnit.SECONDS);
			if (future != null) {
				nbEnCours.decrementAndGet();

				try {
					final MigrationResult res = future.get();
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Résultat de migration reçu : " + res);
					}

					// utilisation des loggers pour les fichiers/listes de contrôle
					for (MigrationResult.CategorieListe cat : MigrationResult.CategorieListe.values()) {
						final List<MigrationResult.Message> messages = res.getMessages(cat);
						if (!messages.isEmpty()) {
							final Logger logger = LoggerFactory.getLogger(String.format("%s.%s", MigrationResult.CategorieListe.class.getName(), cat.name()));
							messages.forEach(msg -> log(logger, msg.niveau, msg.texte));
						}
					}
				}
				catch (ExecutionException e) {
					LOGGER.error("Exception inattendue", e.getCause());
				}
			}
		}
	}

	private static void log(Logger logger, MigrationResult.NiveauMessage niveau, String msg) {
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

	private class MigrationTask implements Callable<MigrationResult> {
		private final Graphe graphe;

		private MigrationTask(Graphe graphe) {
			this.graphe = graphe;
		}

		@Override
		public MigrationResult call() {
			final Set<Long> idsEntreprise = graphe.getEntreprises().keySet();
			final Set<Long> idsIndividus = graphe.getIndividus().keySet();
			try {
				while (true) {
					final EntityMigrationSynchronizer.Ticket ticket = synchronizer.hold(idsEntreprise, idsIndividus, 1000);
					if (ticket != null) {
						try {
							return migrate(graphe);
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
				res.addMessage(MigrationResult.CategorieListe.ERREUR_GENERIQUE, MigrationResult.NiveauMessage.ERROR, msg);
				return res;
			}
		}
	}

	private static String dump(Throwable t) {
		try (StringWriter sw = new StringWriter(); PrintWriter pw = new PrintWriter(sw)) {
			pw.print(t.getClass().getName());
			pw.print(": ");
			pw.println(t.getMessage());
			t.printStackTrace(pw);
			pw.flush();
			return sw.toString();
		}
		catch (IOException e) {
			LOGGER.error("Pas pu générer le message d'erreur pour l'exception reçue", t);
			return "Erreur inattendue (voir logs applicatifs à " + new Date() + ")";
		}
	}

	private MigrationResult migrate(Graphe graphe) throws MigrationException {
		// TODO à implémenter...
		try {
			Thread.sleep(5000);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		final MigrationResult mr = new MigrationResult();
		graphe.getEntreprises().keySet().forEach(id -> mr.addMessage(MigrationResult.CategorieListe.PM_MIGREE, MigrationResult.NiveauMessage.INFO, String.format("Entreprise %d migrée", id)));
		return mr;
	}
}
