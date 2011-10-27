package ch.vd.uniregctb.worker;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.mutable.MutableBoolean;
import org.apache.log4j.Logger;

import ch.vd.uniregctb.common.ProgrammingException;

public class WorkingQueue<T> {

	private static final Logger LOGGER = Logger.getLogger(WorkingQueue.class);

	private static class Element<T> {
		private long creationTime = System.nanoTime();
		private T data;

		private Element(T data) {
			this.data = data;
		}

		public long getCreationTime() {
			return creationTime;
		}

		@Override
		public String toString() {
			return "Element{" +
					"creationTime=" + creationTime +
					", data=" + data +
					'}';
		}
	}

	public static class WorkStats {
		long cpuTime;
		long userTime;
		long execTime;

		int threadsCount;
		int deadThreadsCount;

		public WorkStats(int threadsCount) {
			this.threadsCount = threadsCount;
		}

		public void add(long cpuTime, long userTime, long execTime) {
			this.cpuTime += cpuTime;
			this.userTime += userTime;
			this.execTime += execTime;
		}

		public void incDeadThreadsCount() {
			++deadThreadsCount;
		}

		public long getCpuTime() {
			return cpuTime;
		}

		public long getUserTime() {
			return userTime;
		}

		public long getExecTime() {
			return execTime;
		}

		public int getDeadThreadsCount() {
			return deadThreadsCount;
		}

		public int getThreadsCount() {
			return threadsCount;
		}
	}

	private final BlockingQueue<Element<T>> queue;
	private final Set<Element<T>> inprocessing;
	private final List<Listener<T>> listeners;
	private FailureNotifier<T> failureNotifier;

	/**
	 * Une queue de travail de longueur illimitée.
	 *
	 * @param nbThreads le nombre de threads workers simultanés
	 * @param worker    la définition du worker
	 */
	public WorkingQueue(int nbThreads, SimpleWorker<T> worker) {
		this.queue = new LinkedBlockingQueue<Element<T>>();
		this.inprocessing = new HashSet<Element<T>>();
		this.listeners = new ArrayList<Listener<T>>();
		for (int i = 0; i < nbThreads; ++i) {
			SimpleListener<T> l = new SimpleListener<T>(this, queue, worker);
			l.setName(worker.getName() + "-" + i);
			this.listeners.add(l);
		}
	}

	/**
	 * Une queue de travail de longueur illimitée.
	 *
	 * @param nbThreads le nombre de threads workers simultanés
	 * @param worker    la définition du worker
	 */
	public WorkingQueue(int nbThreads, BatchWorker<T> worker) {
		this.queue = new LinkedBlockingQueue<Element<T>>();
		this.inprocessing = new HashSet<Element<T>>();
		this.listeners = new ArrayList<Listener<T>>();
		for (int i = 0; i < nbThreads; ++i) {
			BatchListener<T> l = new BatchListener<T>(this, queue, worker);
			l.setName(worker.getName() + "-" + i);
			this.listeners.add(l);
		}
	}

	public WorkingQueue(int capacity, int nbThreads, SimpleWorker<T> worker) {
		this.queue = new ArrayBlockingQueue<Element<T>>(capacity);
		this.inprocessing = new HashSet<Element<T>>(capacity);
		this.listeners = new ArrayList<Listener<T>>();
		for (int i = 0; i < nbThreads; ++i) {
			SimpleListener<T> l = new SimpleListener<T>(this, queue, worker);
			l.setName(worker.getName() + "-" + i);
			this.listeners.add(l);
		}
	}

	public WorkingQueue(int capacity, int nbThreads, BatchWorker<T> worker) {
		this.queue = new ArrayBlockingQueue<Element<T>>(capacity);
		this.inprocessing = new HashSet<Element<T>>(capacity);
		this.listeners = new ArrayList<Listener<T>>();
		for (int i = 0; i < nbThreads; ++i) {
			BatchListener<T> l = new BatchListener<T>(this, queue, worker);
			l.setName(worker.getName() + "-" + i);
			this.listeners.add(l);
		}
	}

	public void setFailureNotifier(FailureNotifier<T> failureNotifier) {
		this.failureNotifier = failureNotifier;
	}

	public void start() {
		synchronized (listeners) {
			for (Listener<T> listener : listeners) {
				LOGGER.info("Démarrage du thread " + listener.getName());
				listener.start();
			}
		}
	}

	public void put(T data) throws InterruptedException {

		final Element<T> d = new Element<T>(data);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("inprocessing.add(" + d + ")");
		}

		synchronized (inprocessing) {
			inprocessing.add(d);
		}

		queue.put(d);
	}

	public boolean offer(T data, long timeout, TimeUnit unit) throws InterruptedException {

		final Element<T> d = new Element<T>(data);

		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("inprocessing.add(" + d + ")");
		}

		synchronized (inprocessing) {
			inprocessing.add(d);
		}

		boolean res = false;
		try {
			res = queue.offer(d, timeout, unit);
		}
		finally {
			if (!res) {
				if (LOGGER.isTraceEnabled()) {
					LOGGER.trace("inprocessing.remove(" + d + ")");
				}
				synchronized (inprocessing) {
					inprocessing.remove(d);
				}
			}
		}

		return res;
	}

	/**
	 * @return <b>vrai</b> si au moins un des workers est vivant; <b>faux</b> s'il n'y a aucun worker ou si ils sont tous morts.
	 */
	public boolean anyWorkerAlive() {
		boolean alive = false;
		synchronized (listeners) {
			for (Thread t : listeners) {
				alive |= (t != null && t.isAlive());
			}
		}
		return alive;
	}

	/**
	 * @return le nombre d'élément actuellement dans la queue
	 */
	public int size() {
		return queue.size();
	}

	/**
	 * @return le nombre de worker actuellement actifs
	 */
	public int workersCount() {
		return listeners.size();
	}

	/**
	 * Supprime le dernier worker de la liste.
	 *
	 * @return le nom du thread qui contenait le worker supprimé.
	 * @throws DeadThreadException si le worker était déjà mort
	 */
	public String removeLastWorker() throws DeadThreadException {
		final Listener<T> last;
		synchronized (listeners) {
			if (listeners.size() <= 1) {
				throw new RuntimeException("Il y a moins de 2 workers actifs actuellement");
			}
			last = listeners.remove(listeners.size() - 1);
		}
		last.shutdown(); // il faut appeler le shutdown en dehors du bloc synchronized, pour éviter des deadlocks dans certaines situations particulières
		return last.getName();
	}

	/**
	 * Ajoute un nouveau worker à la liste et démarre le thread immédiatement.
	 *
	 * @param worker le nouveau worker à ajouter
	 * @return le nom du thread qui contient le nouveau worker
	 */
	public String addNewWorker(SimpleWorker<T> worker) {
		synchronized (listeners) {
			final SimpleListener<T> l = new SimpleListener<T>(this, queue, worker);
			final String name = worker.getName() + "-" + listeners.size();
			l.setName(name);
			this.listeners.add(l);
			l.start();
			return name;
		}
	}

	/**
	 * Ajoute un nouveau worker à la liste et démarre le thread immédiatement.
	 *
	 * @param worker le nouveau worker à ajouter
	 * @return le nom du thread qui contient le nouveau worker
	 */
	public String addNewWorker(BatchWorker<T> worker) {
		synchronized (listeners) {
			final BatchListener<T> l = new BatchListener<T>(this, queue, worker);
			final String name = worker.getName() + "-" + listeners.size();
			l.setName(name);
			this.listeners.add(l);
			l.start();
			return name;
		}
	}

	/**
	 * Détecte et supprime les workers morts.
	 */
	public void purgeDeadWorkers() {
		synchronized (listeners) {
			for (int i = listeners.size() - 1; i >= 0; i--) {
				final Listener<T> l = listeners.get(i);
				if (!l.isAlive()) {
					LOGGER.warn("Détecté que le worker " + l.getName() + " est mort.");
					listeners.remove(i);
				}
			}
		}
	}

	/**
	 * Vide la queue et transmet l'ordre de reset à tous les workers. Au retour de cette méthode, la queue est vide et tous les workers sont en attente de travail.
	 */
	public void reset() {

		// on vide la queue dans une liste
		final List<Element<T>> drain = new ArrayList<Element<T>>();
		queue.drainTo(drain);
		
		// on supprime tous les éléments de la liste (les éléments non-présents dans le queue mais présents dans le 'inprocessing' sont ceux en cours de traitement par les listeners)
		if (!drain.isEmpty()) {
			removeFromInProcessing(drain);
		}

		// on travaille sur une copie de la liste pour éviter de partir en dead-lock lors de l'appel du reset()
		final List<Listener<T>> copy;
		synchronized (listeners) {
			copy = new ArrayList<Listener<T>>(listeners);
		}

		for (Listener<T> listener : copy) {
			try {
				listener.reset();
			}
			catch (DeadThreadException e) {
				LOGGER.error("Le thread [" + listener.getName() + "] était déjà mort lors de l'appel de reset()", e); // que faire d'autre ?
			}
		}
	}

	/**
	 * Stoppe le processing des éléments : attends que tous les workers aient terminés les éléments actuellement dans la queue, arrête les workers, supprime tous les workers et retourne les statistiques
	 * de runtime.
	 *
	 * @return les statistiques consolidées du runtime des workers
	 */
	public WorkStats shutdown() {

		// processing de tous les éléments actuellement dans la queue
		try {
			sync();
		}
		catch (DeadThreadException e) {
			// on ignore l'exception, on est de toutes façons entrain de s'arrêter
		}

		// on travaille sur une copie de la liste pour éviter de partir en dead-lock lors de l'appel du shutdown()
		final List<Listener<T>> copy;
		synchronized (listeners) {
			copy = new ArrayList<Listener<T>>(listeners);
		}

		final WorkStats stats = new WorkStats(copy.size());

		// arrêt des workers
		for (Listener<T> listener : copy) {
			try {
				listener.shutdown();
			}
			catch (DeadThreadException e) {
				LOGGER.debug("Détecté que le listener [" + listener.getName() + "] est mort avant la demande d'arrêt.");
				stats.incDeadThreadsCount();
			}
		}

		// récupération des stats des workers
		final ThreadMXBean mXBean = ManagementFactory.getThreadMXBean();
		for (Listener<T> listener : copy) {
			final long cpuTime = mXBean.getThreadCpuTime(listener.getId());
			final long userTime = mXBean.getThreadUserTime(listener.getId());

			try {
				listener.join(10000);
			}
			catch (InterruptedException e) {
				// attente interrompue... pas grave, on aura attendu moins longtemps, c'est tout!
			}
			if (listener.isAlive()) {
				LOGGER.warn("Interruption forcée du thread " + listener.getName() + " qui ne s'est pas arrêté après 10 secondes d'attente.");
				listener.interrupt();
			}

			final long execTime = listener.getExecutionTime();
			stats.add(cpuTime, userTime, execTime);
		}

		// cleanup
		listeners.clear();
		queue.clear();
		inprocessing.clear();

		return stats;
	}

	/**
	 * Cette méthode ne retourne que lorsque tous les éléments déjà présents dans le queue au moment de l'appel sont processés. En fonction du temps de la stratégie de scheduling, il est possible que des
	 * éléments insérés après dans le queue après l'appel de sync() soient processés.
	 *
	 * @throws DeadThreadException si tous les workers sont morts, ou qu'il n'y pas de workers définis.
	 */
	public void sync() throws DeadThreadException {
		final long syncTime = System.nanoTime();
		synchronized (inprocessing) {
			while (!allProcessedUpTo(syncTime)) {
				if (!anyWorkerAlive()) {
					throw new DeadThreadException();
				}
				try {
					inprocessing.wait(1000L); // attentes de 1s, pour vérifier périodiquement que tout va bien
				}
				catch (InterruptedException e) {
					throw new RuntimeException(e);
				}
			}
		}
	}

	private void processingFailed(Element<T> d) {
		// le processing de l'élément n'a pas fonctionné (peut-être un problème sur un des workers), on le notifie
		if (failureNotifier != null) {
			failureNotifier.processingFailed(d.data);
		}
		// et on enlève l'éléments concerné
		removeFromInProcessing(d);
	}

	private void processingFailed(List<Element<T>> list) {
		// le processing du lot n'a pas fonctionné (peut-être un problème sur un des workers), on le notifie
		if (failureNotifier != null) {
			final List<T> l = new ArrayList<T>(list.size());
			for (Element<T> e : list) {
				l.add(e.data);
			}
			failureNotifier.processingFailed(l);
		}
		// et on enlève les éléments concernés
		removeFromInProcessing(list);
	}

	private void processingSuccessful(Element<T> e) {
		// le lot a été processé, on l'enlève du set
		removeFromInProcessing(e);
	}

	private void processingSuccessful(List<Element<T>> list) {
		// le lot a été processé, on l'enlève du set
		removeFromInProcessing(list);
	}

	private void removeFromInProcessing(Element<T> e) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("inprocessing.remove(" + e + ")");
		}
		synchronized (inprocessing) {
			if (!inprocessing.remove(e)) {
				throw new ProgrammingException("L'élément [" + e + "] n'est pas dans le set inprocessing !");
			}
			inprocessing.notifyAll(); // on notifie les éventuels processus en attente de changement
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("done");
		}
	}

	private void removeFromInProcessing(List<Element<T>> list) {
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("inprocessing.remove(" + Arrays.toString(list.toArray()) + ")");
		}
		synchronized (inprocessing) {
			for (Element<T> e : list) {
				if (!inprocessing.remove(e)) {
					throw new ProgrammingException("L'élément [" + e + "] n'est pas dans le set inprocessing !");
				}
			}
			inprocessing.notifyAll(); // on notifie les éventuels processus en attente de changement
		}
		if (LOGGER.isTraceEnabled()) {
			LOGGER.trace("done");
		}
	}

	private boolean allProcessedUpTo(long syncTime) {
		boolean allProcessed = true;
		for (Element<T> element : inprocessing) {
			if (element.getCreationTime() < syncTime) {
				allProcessed = false;
				break;
			}
		}
		if (!allProcessed && LOGGER.isTraceEnabled()) {
			LOGGER.trace("inprocessing = " + Arrays.toString(inprocessing.toArray()));
		}
		return allProcessed;
	}

	private static abstract class Listener<T> extends Thread {

		protected final WorkingQueue<T> master;
		protected final BlockingQueue<Element<T>> queue;
		protected boolean shutdown = false;
		private final MutableBoolean processingDone = new MutableBoolean(false);

		private long executionTime = 0;

		private Listener(WorkingQueue<T> master, BlockingQueue<Element<T>> queue) {
			this.master = master;
			this.queue = queue;
		}

		@Override
		public void run() {
			long start = System.nanoTime();
			try {
				doRun();
			}
			catch (Exception e) {
				LOGGER.error(e);
			}
			finally {
				executionTime = System.nanoTime() - start;
				LOGGER.info("Arrêt du thread.");
				notifyProcessingDone();
			}
		}

		protected abstract void doRun();

		/**
		 * Termine de processer l'élément courant et retourne lorsque le thread est de nouveau en attente
		 *
		 * @throws DeadThreadException si la synchronisation n'est pas possible car le thread est mort sans finir son travail
		 */
		public void reset() throws DeadThreadException {
			sync(); // on attend que le thread soit de nouveau en attente
		}

		/**
		 * Demande au thread de s'arrêter. Le thread termine de vider la queue dans tous les cas.
		 *
		 * @throws DeadThreadException si la synchronisation n'est pas possible car le thread est mort sans finir son travail
		 */
		public void shutdown() throws DeadThreadException {
			if (!shutdown) {
				if (!isAlive()) {
					throw new DeadThreadException(getName());
				}
				shutdown = true; // à partir de là, le thread peut s'arrêter tout seul
				try {
					sync();
				}
				catch (DeadThreadException e) {
					// si le thread s'est arrêté tout seul avant le sync, on reçoit cette exception qu'on peut ignorer sans soucis
				}
			}
		}

		public long getExecutionTime() {
			return executionTime;
		}

		/**
		 * Attend que l'élément courant ait été processé.
		 *
		 * @throws DeadThreadException si la synchronisation n'est pas possible car le thread est mort sans finir son travail
		 */
		private void sync() throws DeadThreadException {
			try {
				synchronized (processingDone) {
					processingDone.setValue(false);
					while (!processingDone.booleanValue()) {
						processingDone.wait(1000L);         // attente de 1s, pour vérifier périodiquement que tout va bien

						// si le thread es mort, de deux choses l'une :
						// 1. ou bien tout le traitement a été fait, auquel cas processingDone a changé d'état (donc la prochaine boucle se sera pas exécutée), et tout va bien
						// 2. ou bien le thread a sauté, et processingDone ne changera JAMAIS PLUS d'état... pas la peine d'attendre la fin du monde...
						if (!isAlive() && !processingDone.booleanValue()) {
							throw new DeadThreadException(getName());
						}
					}
				}
			}
			catch (InterruptedException e) {
				// on ignore cette exception
			}
		}

		protected void notifyProcessingDone() {
			synchronized (processingDone) {
				processingDone.setValue(true);
				processingDone.notifyAll(); // notifie les threads en attente sur 'sync' qu'on a fini (momentanément) d'indexer tous les tiers de la queue
			}
		}
	}

	private static class SimpleListener<T> extends Listener<T> {

		private final SimpleWorker<T> worker;

		private SimpleListener(WorkingQueue<T> master, BlockingQueue<Element<T>> queue, SimpleWorker<T> worker) {
			super(master, queue);
			this.worker = worker;
		}

		@Override
		public void doRun() {
			while (!shutdown || !queue.isEmpty()) {
				final Element<T> e = poll();
				if (e == null) {
					notifyProcessingDone();
					continue;
				}
				try {
					worker.process(e.data);
					master.processingSuccessful(e);
				}
				catch (Exception exception) {
					LOGGER.error("Erreur dans le traitement de l'élément [" + e + "]", exception);
					master.processingFailed(e);
					throw new RuntimeException(exception);
				}
				finally {
					notifyProcessingDone();
				}
			}
		}

		private Element<T> poll() {
			try {
				return queue.poll(100, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				interrupted(); // reset le flag
			}
			return null;
		}
	}

	private static class BatchListener<T> extends Listener<T> {

		private final BatchWorker<T> worker;
		private int batchSize;

		private BatchListener(WorkingQueue<T> master, BlockingQueue<Element<T>> queue, BatchWorker<T> worker) {
			super(master, queue);
			this.worker = worker;
			this.batchSize = worker.maxBatchSize();
		}

		@Override
		protected void doRun() {
			while (!shutdown || !queue.isEmpty()) {
				final List<Element<T>> list = next();
				if (list == null) {
					notifyProcessingDone();
					continue;
				}
				try {
					List<T> batch = new ArrayList<T>(list.size());
					for (Element<T> e : list) {
						batch.add(e.data);
					}
					worker.process(batch);
					master.processingSuccessful(list);
				}
				catch (Exception e) {
					LOGGER.error("Erreur dans le traitement du lot [" + Arrays.toString(list.toArray()) + "]", e);
					master.processingFailed(list);
					throw new RuntimeException(e);
				}
				finally {
					notifyProcessingDone();
				}
			}
		}

		/**
		 * @return le prochain lot d'éléments à traiter, ou <b>null</b> s'il n'y a (momentanément) plus rien à faire (timeout de 0.1 seconde)
		 */
		private List<Element<T>> next() {

			List<Element<T>> batch = null;

			for (int i = 0; i < batchSize; i++) {
				final Element<T> id = poll();
				if (id == null) {
					// la queue est vide (peut-être momentanément), on va comme ça avec ce batch
					break;
				}
				if (batch == null) {
					batch = new ArrayList<Element<T>>(batchSize);
				}
				batch.add(id);
			}

			return batch;
		}

		/**
		 * @return le prochain élément à traiter, ou <b>null</b> s'il n'y a (momentanément) plus rien à faire (timeout de 0.1 seconde)
		 */
		private Element<T> poll() {
			try {
				return queue.poll(100, TimeUnit.MILLISECONDS);
			}
			catch (InterruptedException e) {
				interrupted(); // reset le flag
				//LOGGER.error(e, e);
				return null;
			}
		}

	}

}
