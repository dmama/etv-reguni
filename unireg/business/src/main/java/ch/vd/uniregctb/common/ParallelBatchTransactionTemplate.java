package ch.vd.uniregctb.common;

import org.apache.log4j.Logger;
import org.springframework.orm.hibernate3.HibernateTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

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

	private boolean readonly;
	private final ArrayBlockingQueue<List<E>> queue;
	private int percent;

	/**
	 * @param elements           les éléments à traiter par lots et en parallèle
	 * @param batchSize          taille maximale des lots
	 * @param nbThreads          nombre de threads à démarrer pour le traitement en parallèle
	 * @param behavior           comportement du template en cas d'exception levée dans une transaction
	 * @param transactionManager le transaction manager Spring
	 * @param statusManager      un status manager
	 * @param hibernateTemplate  le template Hibernate Spring
	 */
	public ParallelBatchTransactionTemplate(List<E> elements, int batchSize, int nbThreads, BatchTransactionTemplate.Behavior behavior, PlatformTransactionManager transactionManager,
	                                        StatusManager statusManager,
	                                        HibernateTemplate hibernateTemplate) {
		this.elements = ListUtils.split(elements, batchSize);
		this.nbThreads = nbThreads;
		this.transactionManager = transactionManager;
		this.behavior = behavior;
		this.statusManager = statusManager;
		this.hibernateTemplate = hibernateTemplate;

		this.queue = new ArrayBlockingQueue<List<E>>(5);
		this.percent = 0;
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
			final R syncRapport = (rapportFinal == null ? null : SynchronizedFactory.makeSynchronized(BatchResults.class, rapportFinal));
			return executeParallel(syncRapport, action);
		}
		catch (InterruptedException e) {
			LOGGER.error(e, e);
			throw new RuntimeException(e);
		}
	}

	private boolean executeParallel(R rapportFinal, BatchTransactionTemplate.BatchCallback<E, R> action) throws InterruptedException {

		// Démarre les threads
		final List<BatchThread> threads = new ArrayList<BatchThread>(nbThreads);
		for (int i = 0; i < nbThreads; ++i) {
			final BatchThread thread = new BatchThread(rapportFinal, action, AuthenticationHelper.getCurrentPrincipal());
			thread.setName(String.format("%s-%d", Thread.currentThread().getName(), i));
			thread.start();
			threads.add(thread);
		}

		// Transmet les éléments à processer
		final int size = elements.size();
		for (int i = 0; i < size; ++i) {
			final List<E> element = elements.get(i);
			while (!queue.offer(element, 2, TimeUnit.SECONDS) && !interrupted(threads)) { // on ne bloque pas complétement de manière à détecter si les threads sont morts
				assertThreadsAlives(threads);
			}
			if (interrupted(threads)) {
				break;
			}
			percent = ((i - queue.size()) * 100 / size);
		}

		// Attend la fin du processing
		while (!queue.isEmpty() && !interrupted(threads)) {
			assertThreadsAlives(threads);
			Thread.sleep(200);
		}

		// Termine les threads.
		for (BatchThread thread : threads) {
			thread.done();
		}
		for (BatchThread thread : threads) {
			thread.join();
		}

		return !interrupted(threads);
	}

	/**
	 * @param threads les threads de traitement
	 * @return <i>vrai</i> si le traitement a été interrompu (par le status manager, ou si tous les threads ont interrompu eux-mêmes leurs traitements).
	 */
	private boolean interrupted(List<BatchThread> threads) {
		return (statusManager != null && statusManager.interrupted()) || allInterrupted(threads);
	}

	/**
	 * @param threads les threads de traitement
	 * @return <i>vrai</i> si tous les threads ont interrompu eux-mêmes leurs traitements.
	 */
	private boolean allInterrupted(List<BatchThread> threads) {
		for (BatchThread t : threads) {
			if (!t.interruptedItself) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Vérifie que - au minimum - un thread est toujours vivant; autrement lève une exception.
	 *
	 * @param threads les threads à tester
	 */
	private static void assertThreadsAlives(List<? extends Thread> threads) {
		boolean alive = false;
		for (Thread t : threads) {
			alive = alive || t.isAlive();
		}
		if (!alive) {
			throw new RuntimeException("Tous les threads de processing sont morts avant de finir le traitement.");
		}
	}

	/**
	 * Thread utilisé pour le traitement des lots.
	 * <p/>
	 * Ce thread lit les lots à processer dans la queue, et travaille tant que la méthode {@link #done()} n'a pas été appelée.
	 */
	private class BatchThread extends Thread {

		private final String principal;
		private R rapportFinal;
		private final BatchTransactionTemplate.BatchCallback<E, R> action;
		private final BlockingQueueIterator iterator = new BlockingQueueIterator();

		private boolean interruptedItself = false;

		private BatchThread(R rapportFinal, BatchTransactionTemplate.BatchCallback<E, R> action, String principal) {
			this.rapportFinal = rapportFinal;
			this.action = action;
			this.principal = principal;
		}

		/**
		 * Interrompt le travail du thread, qui va encore finir de processer le lot courant avant de s'arrêter.
		 */
		public void done() {
			iterator.done();
		}

		@Override
		public void run() {
			AuthenticationHelper.pushPrincipal(principal);
			try {
				BatchTransactionTemplate<E, R> template = new BatchTransactionTemplate<E, R>(iterator, behavior, transactionManager, statusManager, hibernateTemplate);
				template.setReadonly(readonly);
				interruptedItself = !template.execute(rapportFinal, action);
			}
			finally {
				AuthenticationHelper.popPrincipal();
			}
		}
	}

	/**
	 * Itérateur spécialisé qui extrait les données à partir de la queue (bloquante) des lots à traiter.
	 */
	private class BlockingQueueIterator implements BatchIterator<E> {

		private boolean done = false;

		/**
		 * Provoque l'arrêt de la lecture de la queue, et retourne <i>null</i> au prochain appel de {@link #next()}.
		 */
		public void done() {
			done = true;
		}

		public boolean hasNext() {
			// on ne peut pas deviner à l'avance s'il restera des données dans la queue, on retourne oui ici et on retournera null dans 'next' s'il n'y a plus de données
			return !done;
		}

		public Iterator<E> next() {
			try {
				List<E> list = null;
				while (!done && list == null) {
					list = queue.poll(1, TimeUnit.SECONDS);
				}
				return list == null ? null : list.iterator();
			}
			catch (InterruptedException e) {
				// plus de données
				return null;
			}
		}

		public int getPercent() {
			return percent;
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setReadonly(boolean readonly) {
		this.readonly = readonly;
	}
}
