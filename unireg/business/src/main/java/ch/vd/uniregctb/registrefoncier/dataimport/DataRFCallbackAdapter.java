package ch.vd.uniregctb.registrefoncier.dataimport;

import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.Bodenbedeckung;
import ch.vd.capitastra.grundstueck.Gebaeude;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.Personstamm;

/**
 * Classe qui transforme un flux de données reçues par callback en des itérateurs traditionnels. Cette classe nécessite deux threads au minimum pour fonctionner : un thread pour envoyer les éléments (Callback) et un thread pour itérer.
 */
public class DataRFCallbackAdapter implements FichierImmeublesRFParser.Callback {

	// on ne garde que 100 éléments en mémoire, ça semble suffisant
	private static final int QUEUE_SIZE = 100;

	private static class QueuedIterator<T> implements Iterator<T> {

		private final BlockingQueue<T> queue = new ArrayBlockingQueue<>(QUEUE_SIZE);
		private boolean done = false;

		public void put(T o) {
			try {
				queue.put(o);
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		public void done() {
			done = true;
		}

		/**
		 * @return <b>vrai</b> s'il y a <i>peut-être</i> encore un ou des éléments à traiter; <b>faux</b> s'il est certain qu'il n'y a plus d'éléments à traiter.
		 */
		@Override
		public boolean hasNext() {
			return !queue.isEmpty() || !done;
		}

		@Override
		public T next() {
			try {
				T o;
				do {
					o = queue.poll(100, TimeUnit.MILLISECONDS);
				} while (o == null && !done);
				return o;
			}
			catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private final QueuedIterator<Grundstueck> immeublesIterator = new QueuedIterator<>();
	private final QueuedIterator<PersonEigentumAnteil> droitsIterator = new QueuedIterator<>();
	private final QueuedIterator<Personstamm> proprietairesIterator = new QueuedIterator<>();
	private final QueuedIterator<Gebaeude> constructionsIterator = new QueuedIterator<>();
	private final QueuedIterator<Bodenbedeckung> surfacesIterator = new QueuedIterator<>();

	public DataRFCallbackAdapter() {
	}

	/**
	 * @return un itérateur sur les immeubles.
	 */
	public Iterator<Grundstueck> getImmeublesIterator() {
		return immeublesIterator;
	}

	/**
	 * @return un itérateur sur les droits.
	 */
	public Iterator<PersonEigentumAnteil> getDroitsIterator() {
		return droitsIterator;
	}

	/**
	 * @return un itérateur sur les propriétaires.
	 */
	public Iterator<Personstamm> getProprietairesIterator() {
		return proprietairesIterator;
	}

	/**
	 * @return un itérateur sur les constructions.
	 */
	public Iterator<Gebaeude> getConstructionsIterator() {
		return constructionsIterator;
	}

	/**
	 * @return un itérateur sur les surfaces.
	 */
	public Iterator<Bodenbedeckung> getSurfacesIterator() {
		return surfacesIterator;
	}

	@Override
	public void onImmeuble(@NotNull Grundstueck immeuble) {
		immeublesIterator.put(immeuble);
	}

	@Override
	public void onDroit(@NotNull PersonEigentumAnteil droit) {
		immeublesIterator.done();
		droitsIterator.put(droit);
	}

	@Override
	public void onProprietaire(@NotNull Personstamm personne) {
		immeublesIterator.done();
		droitsIterator.done();
		proprietairesIterator.put(personne);
	}

	@Override
	public void onBatiment(@NotNull Gebaeude construction) {
		immeublesIterator.done();
		droitsIterator.done();
		proprietairesIterator.done();
		constructionsIterator.put(construction);
	}

	@Override
	public void onSurface(@NotNull Bodenbedeckung surface) {
		immeublesIterator.done();
		droitsIterator.done();
		proprietairesIterator.done();
		constructionsIterator.done();
		surfacesIterator.put(surface);
	}

	@Override
	public void done() {
		immeublesIterator.done();
		droitsIterator.done();
		proprietairesIterator.done();
		constructionsIterator.done();
		surfacesIterator.done();
	}
}
