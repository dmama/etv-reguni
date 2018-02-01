package ch.vd.unireg.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

/**
 * Classe qui joue le rôle d'une façade sur un {@link ExecutorService} dégénéré (toutes les méthodes ne sont pas reprises)
 * en permettant de savoir qui est en attente et qui est en train d'être exécuté
 * @param <T> le type renvoyé par les tâches exécutées
 * @param <C> la classe des tâches exécutées
 */
public class MonitorableExecutorService<T, C extends Callable<T>> {

	private final ExecutorService executorService;

	private final Map<IdentityKey<C>, Future<T>> waiting = new LinkedHashMap<>();
	private final Map<IdentityKey<C>, Future<T>> running = new LinkedHashMap<>();
	private final Object sync = new Object();

	public MonitorableExecutorService(ExecutorService executorService) {
		this.executorService = executorService;
	}

	/**
	 * Aucune nouvelle soumission ne sera acceptée après cet appel,
	 * et les tâches actuellement en attente sont annulées.
	 * @see ExecutorService#shutdown()
	 */
	public void shutdown() {
		executorService.shutdown();

		synchronized (sync) {
			// cancel all waiting elements
			for (Future<T> future : waiting.values()) {
				future.cancel(false);
			}
		}
	}

	/**
	 * Attend le temps déterminé au maximum que l'exécutor soit terminé
	 * @param timeout temps maximal d'attente
	 * @param unit unité du temps d'attente
	 * @return <code>true</code> si l'exécuteur est terminé, false si le <i>timeout</i> a sonné
	 * @throws InterruptedException
	 * @see ExecutorService#awaitTermination(long, java.util.concurrent.TimeUnit)
	 */
	public boolean awaitTermination(long timeout, @NotNull TimeUnit unit) throws InterruptedException {
		return executorService.awaitTermination(timeout, unit);
	}

	/**
	 * Wrapper de {@link Callable} qui gère les collections de tâches en attente et en cours d'exécution
	 */
	private class CallableWrapper implements Callable<T> {

		private final IdentityKey<C> callable;

		private CallableWrapper(C task) {
			this.callable = new IdentityKey<>(task);
		}

		@Override
		public T call() throws Exception {
			synchronized (sync) {
				final Future<T> future = waiting.remove(callable);
				running.put(callable, future);
			}
			try {
				return callable.getElt().call();
			}
			finally {
				synchronized (sync) {
					running.remove(callable);
				}
			}
		}
	}

	/**
	 * Soumission d'une nouvelle tâche à exécuter
	 * @param task la tâche à exécuter
	 * @return un {@link Future} qui représente l'état de complétion de la tâche
	 * @see ExecutorService#submit(java.util.concurrent.Callable)
	 */
	@NotNull
	public Future<T> submit(@NotNull C task) {
		final CallableWrapper wrapper = new CallableWrapper(task);
		synchronized (sync) {
			final Future<T> future = executorService.submit(wrapper);
			waiting.put(wrapper.callable, future);
			return future;
		}
	}

	/**
	 * @return le nombre de tâche actuellement en attente d'exécution
	 */
	public int getWaitingSize() {
		synchronized (sync) {
			return waiting.size();
		}
	}

	/**
	 * @return les tâches actuellement en attente d'exécution (photo)
	 */
	public Collection<C> getWaiting() {
		return extractCollection(waiting);
	}

	/**
	 * @return les tâches actuellement en cours d'exécution (photo)
	 */
	public Collection<C> getRunning() {
		return extractCollection(running);
	}

	private Collection<C> extractCollection(Map<IdentityKey<C>, Future<T>> src) {
		synchronized (sync) {
			final List<C> result = new ArrayList<>(src.size());
			for (IdentityKey<C> wrapper : src.keySet()) {
				result.add(wrapper.getElt());
			}
			return result;
		}
	}

	/**
	 * Demande d'annulation d'une tâche actuellement en attente (ne fait rien si la tâche est actuellement en cours)
	 * @param task la tâche actuellement en attente
	 */
	public void cancel(@NotNull C task) {
		final IdentityKey<C> wrapper = new IdentityKey<>(task);
		synchronized (sync) {
			final Future<T> future = waiting.remove(wrapper);
			if (future != null) {
				future.cancel(false);
			}
		}
	}
}
