package ch.vd.uniregctb.common;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class OtherPrincipalSynchronousExecutor {

	private final ExecutorService executorService;

	public OtherPrincipalSynchronousExecutor(String threadNamePrefix, int minThreads, int maxThreads) {
		this.executorService = new ThreadPoolExecutor(minThreads, maxThreads, 5, TimeUnit.SECONDS,
		                                              new LinkedBlockingQueue<Runnable>(),
		                                              new DefaultThreadFactory(new DefaultThreadNameGenerator(threadNamePrefix)));
	}

	/**
	 * Lance la tâche donnée sur un autre thread, dans le contexte d'authentification du visa donné, en attendant la fin du traitement
	 * @param visa le visa à placer temporairement dans le contexte d'authentification pendant l'exécution de la tâche
	 * @param task la tâche à exécuter
	 */
	public void doWithPrincipal(final String visa, final Runnable task) {
		final Future<?> future = executorService.submit(new Runnable() {
			@Override
			public void run() {
				AuthenticationHelper.pushPrincipal(visa);
				try {
					task.run();
				}
				finally {
					AuthenticationHelper.popPrincipal();
				}
			}
		});
		try {
			try {
				// on attend que la tâche soit terminée
				future.get();
			}
			catch (ExecutionException e) {
				throw e.getCause();
			}
			catch (InterruptedException e) {
				throw new RuntimeException("Thread was interrupted!", e);
			}
		}
		catch (RuntimeException | Error e) {
			throw e;
		}
		catch (Throwable t) {
			throw new RuntimeException(t);      // on ne devrait pas pouvoir passer ici avec un Runnable...
		}
	}

	/**
	 * Appelé une fois que tout est terminé, pour arrêter les threads d'exécution
	 */
	public void shutdown() {
		executorService.shutdown();
	}
}
