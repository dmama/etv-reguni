package ch.vd.unireg.registrefoncier.dataimport;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Iterator qui prend comme valeurs d'entrée des éléments reçus de manière asynchrone.
 */
public class QueuedIterator<T> implements Iterator<T> {

	private final BlockingQueue<T> queue;
	private boolean sourceError = false;
	private boolean done = false;

	public QueuedIterator(int queueSize) {
		queue = new ArrayBlockingQueue<>(queueSize);
	}

	public void put(T o) {
		try {
			queue.put(o);
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Une erreur dans la source des données a été détectée, il faut tout arrêter.
	 */
	public void onSourceError() {
		sourceError = true;
	}

	public void done() {
		done = true;
	}

	/**
	 * @return <b>vrai</b> s'il y a <i>peut-être</i> encore un ou des éléments à traiter; <b>faux</b> s'il est certain qu'il n'y a plus d'éléments à traiter.
	 */
	@Override
	public boolean hasNext() {
		if (sourceError) {
			throw new RuntimeException("Erreur dans la source des données, veuillez consulter le log.");
		}
		return !queue.isEmpty() || !done;
	}

	@Override
	public T next() {
		try {
			T o;
			do {
				if (sourceError) {
					throw new RuntimeException("Erreur dans la source des données, veuillez consulter le log.");
				}
				o = queue.poll(100, TimeUnit.MILLISECONDS);
			} while (o == null && !done);
			return o;
		}
		catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
