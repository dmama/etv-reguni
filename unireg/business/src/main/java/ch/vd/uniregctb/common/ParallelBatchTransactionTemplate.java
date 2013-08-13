package ch.vd.uniregctb.common;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

import ch.vd.uniregctb.hibernate.HibernateTemplate;

/**
 * Classe utilitaire qui reprend la fonctionnalité du {@link ch.vd.uniregctb.common.BatchTransactionTemplate} et ajoute celle de traiter les lots avec <i>n</i> threads en parallèle.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class ParallelBatchTransactionTemplate<E, R extends BatchResults> {

	private static final Logger LOGGER = Logger.getLogger(ParallelBatchTransactionTemplate.class);

	private final List<List<E>> elements;
	private final int nbThreads;
	private final PlatformTransactionManager transactionManager;
	private final BatchTransactionTemplate.Behavior behavior;
	private final StatusManager statusManager;
	private final HibernateTemplate hibernateTemplate;

	private boolean readonly = false;

	/**
	 * @param elements           les éléments à traiter par lots et en parallèle
	 * @param batchSize          taille maximale des lots
	 * @param nbThreads          nombre de threads à démarrer pour le traitement en parallèle
	 * @param behavior           comportement du template en cas d'exception levée dans une transaction
	 * @param transactionManager le transaction manager Spring
	 * @param statusManager      un status manager
	 * @param hibernateTemplate  le template Hibernate Spring
	 */
	public ParallelBatchTransactionTemplate(Collection<E> elements, int batchSize, int nbThreads, BatchTransactionTemplate.Behavior behavior, PlatformTransactionManager transactionManager,
	                                        StatusManager statusManager, HibernateTemplate hibernateTemplate) {
		this.elements = CollectionsUtils.split(elements, batchSize);
		this.nbThreads = nbThreads;
		this.transactionManager = transactionManager;
		this.behavior = behavior;
		this.statusManager = statusManager;
		this.hibernateTemplate = hibernateTemplate;
	}

	/**
	 * Exécute le traitement par batchs dans autant de transactions séparées, en utilisant le nombre de threads spécifiés.
	 * <p/>
	 * <b>Attention !</b> La classe <i>action</i> spécifiée doit être thread-safe !
	 *
	 * @param action l'action à effectué sur les batches.
	 * @return <i>true</i> si le traitement s'est bien effectué jusqu'au bout (éventuellement avec des erreurs); ou <i>faux</i> si le traitement a été interrompu.
	 * @throws org.springframework.transaction.TransactionException
	 *          en cas de problème de transaction
	 */
	public boolean execute(final BatchTransactionTemplate.BatchCallback<E, R> action) throws TransactionException {
		try {
			return executeParallel(null, action);
		}
		catch (InterruptedException e) {
			LOGGER.error(e, e);
			throw new RuntimeException(e);
		}
	}

	/**
	 * Exécute le traitement par batchs dans autant de transactions séparées, en utilisant le nombre de threads spécifiés.
	 * <p/>
	 * <b>Attention !</b> La classe <i>action</i> spécifiée doit être thread-safe !
	 *
	 * @param rapportFinal le rapport d'exécution qui sera complété automatiquement.
	 * @param action       l'action à effectué sur les batches.
	 * @return <i>true</i> si le traitement s'est bien effectué jusqu'au bout (éventuellement avec des erreurs); ou <i>faux</i> si le traitement a été interrompu.
	 * @throws org.springframework.transaction.TransactionException
	 *          en cas de problème de transaction
	 */
	public boolean execute(R rapportFinal, final BatchTransactionTemplate.BatchCallback<E, R> action) throws TransactionException {
		try {
			// on wrappe le rapport autour d'un proxy qui va synchroniser les appels aux méthodes publiques : de cette manière on peut le passer sans soucis aux threads de processing.
			@SuppressWarnings("unchecked") final BatchResults<E, R> syncRapport = (rapportFinal == null ? null : SynchronizedFactory.makeSynchronized(BatchResults.class, rapportFinal));
			return executeParallel(syncRapport, action);
		}
		catch (InterruptedException e) {
			LOGGER.error(e, e);
			throw new RuntimeException(e);
		}
	}

	private static final class TaskMonitor<E> {
		final List<E> inputData;
		final Future<Boolean> future;

		private TaskMonitor(List<E> inputData, Future<Boolean> future) {
			this.inputData = inputData;
			this.future = future;
		}
	}

	private boolean executeParallel(BatchResults<E, R> rapportFinal, BatchTransactionTemplate.BatchCallback<E, R> action) throws InterruptedException {

		final ExecutorService executorService = Executors.newFixedThreadPool(nbThreads, new DefaultThreadFactory(new DefaultThreadNameGenerator(Thread.currentThread().getName())));
		try {

			boolean stop = false;

			// liste des tâches à surveiller
			final List<TaskMonitor<E>> tasks = new LinkedList<>();

			// Transmet les éléments à processer
			final int size = elements.size();
			for (int i = 0; i < size; ++i) {
				final int percent = i * 100 / size;
				final List<E> inputData = elements.get(i);
				if (!interrupted()) {
					tasks.add(new TaskMonitor<>(inputData, executorService.submit(new ParallelTask(inputData, percent, rapportFinal, action, AuthenticationHelper.getCurrentPrincipal()))));
				}
			}

			// maintenant, on attend la fin
			try {
				final Iterator<TaskMonitor<E>> iterator = tasks.iterator();
				while (iterator.hasNext() && !interrupted()) {
					final TaskMonitor<E> task = iterator.next();
					try {
						final Boolean shouldContinue = task.future.get();
						if (shouldContinue != null && !shouldContinue) {
							stop = true;
							break;
						}
					}
					catch (ExecutionException e) {
						final String dataString = buildInputDataString(task.inputData);
						LOGGER.error(String.format("Exception lancée par un des sous-traitements parallélisés : %s", dataString), e.getCause());
					}
					finally {
						iterator.remove();
					}
				}

				return !(stop || interrupted());
			}
			finally {
				// en cas d'interruption, on arrête tout
				if (!tasks.isEmpty()) {
					tasks.clear();
					executorService.shutdownNow();
				}
			}
		}
		finally {
			executorService.shutdown();

			// on attend que tout s'arrête
			//noinspection StatementWithEmptyBody
			while (!executorService.awaitTermination(1, TimeUnit.SECONDS));
		}
	}

	private static <E> String buildInputDataString(List<E> data) {
		final StringBuilder b = new StringBuilder("{");
		for (E elt : data) {
			if (b.length() > 1) {
				b.append(", ");
			}
			b.append(elt == null ? "null" : elt.toString());
		}
		b.append("}");
		return b.toString();
	}

	/**
	 * @return <i>vrai</i> si le traitement a été interrompu (par le status manager).
	 */
	private boolean interrupted() {
		return statusManager != null && statusManager.interrupted();
	}

	private static class DelegatingBatchCallback<E, R extends BatchResults<E, R>> extends BatchTransactionTemplate.BatchCallback<E, R> {

		private final BatchTransactionTemplate.BatchCallback<E, R> target;

		private DelegatingBatchCallback(BatchTransactionTemplate.BatchCallback<E, R> target) {
			this.target = target;
		}

		@Override
		public void beforeTransaction() {
			target.beforeTransaction();
		}

		@Override
		public void afterTransactionStart(TransactionStatus status) {
			target.afterTransactionStart(status);
		}

		public boolean doInTransaction(List<E> batch, R rapport) throws Exception {
			return target.doInTransaction(batch, rapport);
		}

		@Override
		public void afterTransactionCommit() {
			target.afterTransactionCommit();
		}

		@Override
		public void afterTransactionRollback(Exception e, boolean willRetry) {
			target.afterTransactionRollback(e, willRetry);
		}

		@Override
		public R createSubRapport() {
			return target.createSubRapport();
		}
	}

	private class ParallelTask implements Callable<Boolean> {

		private final List<E> input;
		private final int taskPercent;
		private final String principal;
		private final BatchResults<E, R> rapportFinal;
		private final BatchTransactionTemplate.BatchCallback<E, R> action;

		private ParallelTask(List<E> input, int taskPercent, BatchResults<E, R> rapportFinal, BatchTransactionTemplate.BatchCallback<E, R> action, String principal) {
			this.input = input;
			this.taskPercent = taskPercent;
			this.principal = principal;
			this.rapportFinal = rapportFinal;
			this.action = action;
		}

		@Override
		public Boolean call() throws Exception {
			action.percent = taskPercent;
			AuthenticationHelper.pushPrincipal(principal);
			try {
				final BatchTransactionTemplate<E, R> template = new BatchTransactionTemplate<>(input, input.size(), behavior, transactionManager, statusManager, hibernateTemplate);
				template.setReadonly(readonly);
				return template.execute(rapportFinal, new DelegatingBatchCallback<>(action));
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}
}
