package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Iterateur sur une liste qui permet, à tout moment, de connaître les éléments suivants (jusqu'au bout) et précédent (jusqu'au début)
 * @param <E> le type des éléments dans la liste initiale
 */
public class MovingWindow<E> implements Iterator<MovingWindow.Snapshot<E>> {

	private final Iterator<? extends E> iterator;
	private final List<E> nexts;
	private final List<E> previouses;

	/**
	 * A chaque étape de l'iteration, l'état de la fenêtre glissante
	 * @param <E> le type des éléments dans la collection initiale
	 */
	public static final class Snapshot<E> {

		/**
		 * La position centrale courante de la fenêtre
		 */
		private final E current;

		/**
		 * Les éléments après l'élément courant (du plus proche au plus éloigné)
		 */
		@NotNull
		private final List<E> nexts;

		/**
		 * Les éléments avant l'élément courant (du plus proche au plus éloigné -> ordre inversé par rapport à celui de la collection initiale)
		 */
		@NotNull
		private final List<E> previouses;

		private Snapshot(E current, @NotNull List<E> nexts, @NotNull List<E> previouses) {
			this.current = current;
			this.nexts = nexts;
			this.previouses = previouses;
		}

		/**
		 * @return la position courante
		 */
		public E getCurrent() {
			return current;
		}

		/**
		 * @return l'élément qui suit juste celui à la position courante
		 */
		public E getNext() {
			return !nexts.isEmpty() ? nexts.get(0) : null;
		}

		/**
		 * @return l'élément qui suit celui qui suit juste celui de la position courante
		 */
		public E getNextAfterNext() {
			return nexts.size() > 1 ? nexts.get(1) : null;
		}

		/**
		 * @return tous les éléments suivants (du plus proche au plus éloigné --> même ordre que dans la collection initiale)
		 */
		@NotNull
		public List<E> getAllNext() {
			return nexts;
		}

		/**
		 * @return l'élément qui précède juste celui à la position courante
		 */
		public E getPrevious() {
			return !previouses.isEmpty() ? previouses.get(0) : null;
		}

		/**
		 * @return l'élément qui précède celui qui précède juste celui à la position courante
		 */
		public E getPreviousBeforePrevious() {
			return previouses.size() > 1 ? previouses.get(1) : null;
		}

		/**
		 * @return tous les éléments précédant celui de la position courante (du plus proche au plus éloigné --> ordre inversé par rapport à la collection initiale)
		 */
		@NotNull
		public List<E> getAllPrevious() {
			return previouses;
		}
	}

	public MovingWindow(List<? extends E> source) {
		this.iterator = source.iterator();
		this.nexts = source.size() < 2 ? Collections.emptyList() : new LinkedList<>(source.subList(1, source.size()));
		this.previouses = source.isEmpty() ? Collections.emptyList() : new LinkedList<>();
	}

	@Override
	public boolean hasNext() {
		return this.iterator.hasNext();
	}

	@Override
	public Snapshot<E> next() {
		final E current = this.iterator.next();
		final Snapshot<E> snap = new Snapshot<>(current, new ArrayList<>(this.nexts), new ArrayList<>(this.previouses));
		this.previouses.add(0, current);
		if (!this.nexts.isEmpty()) {
			this.nexts.remove(0);
		}
		return snap;
	}

	@Override
	public void remove() {
		this.iterator.remove();
		this.previouses.remove(0);
	}
}
