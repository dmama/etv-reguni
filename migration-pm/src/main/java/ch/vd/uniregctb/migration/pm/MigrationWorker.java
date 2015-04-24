package ch.vd.uniregctb.migration.pm;

import java.util.List;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import ch.vd.uniregctb.common.DefaultThreadFactory;
import ch.vd.uniregctb.common.DefaultThreadNameGenerator;
import ch.vd.uniregctb.migration.pm.adresse.StreetDataMigrator;

public class MigrationWorker implements Worker, InitializingBean, DisposableBean {

	private static final Logger LOGGER = LoggerFactory.getLogger(MigrationWorker.class);

	private static final RejectedExecutionHandler ABORT_POLICY = new ThreadPoolExecutor.AbortPolicy();

	private ExecutorService executor;
	private CompletionService<MigrationResult> completionService;
	private Thread gatheringThread;
	private final AtomicInteger nbEnCours = new AtomicInteger(0);
	private volatile boolean started;

	private StreetDataMigrator streetDataMigrator;

	public void setStreetDataMigrator(StreetDataMigrator streetDataMigrator) {
		this.streetDataMigrator = streetDataMigrator;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.SECONDS,
		                                       new ArrayBlockingQueue<Runnable>(20),
		                                       new DefaultThreadFactory(new DefaultThreadNameGenerator("Migrator")),
		                                       new RejectedExecutionHandler() {
			                                       @Override
			                                       public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
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
		                                       });

		this.completionService = new ExecutorCompletionService<>(this.executor);
		this.gatheringThread = new Thread("Gathering") {
			@Override
			public void run() {
				try {
					gather();
				}
				catch (InterruptedException e) {
					LOGGER.error("Gathering thread interrupted!", e);
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
					// TODO faire quelque chose de ce résultat de migration!
					final MigrationResult res = future.get();
					if (LOGGER.isDebugEnabled()) {
						LOGGER.debug("Résultat de migration reçu : " + res);
					}
				}
				catch (ExecutionException e) {
					final Throwable cause = e.getCause();
					if (cause instanceof MigrationException) {
						// TODO ajouter les données dans le rapport d'erreur
					}
					else {
						// TODO que faire?
					}
				}
			}
		}
	}

	private class MigrationTask implements Callable<MigrationResult> {
		private final Graphe graphe;

		private MigrationTask(Graphe graphe) {
			this.graphe = graphe;
		}

		@Override
		public MigrationResult call() throws MigrationException {
			try {
				// TODO à implémenter...
				Thread.sleep(5000);
				return null;
			}
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				LOGGER.error("Interrupted !");
				throw new RuntimeException("Interrupted !");
			}
		}
	}
}
