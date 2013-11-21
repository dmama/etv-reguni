package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import org.apache.commons.collections.CollectionUtils;

import ch.vd.registre.base.utils.Assert;

public class CollectionsUtils extends CollectionUtils {

	public static interface SplitCallback<T, O> {
		public List<O> process(List<T> list);
	}

	/**
	 * Cette méthode découpe une collection d'éléments en plusieurs sous-listes de taille déterminée, et appelle un callback sur chacune des sous-listes.
	 *
	 * @param collection la collection d'entrée à processer
	 * @param size       la taille (maximale) des sous-listes
	 * @param callback   la méthode de callback appelée sur chacune des sous-listes
	 * @param <T>        le type des éléments contenus dans la collection
	 * @return une collection contenant tous les éléments retournés par les appels aux méthodes 'process' des callbacks.
	 */
	public static <T, O> List<O> splitAndProcess(Collection<T> collection, int size, SplitCallback<T, O> callback) {

		List<O> output = new ArrayList<>();

		Assert.isTrue(size > 0);
		final Iterator<T> iter = collection.iterator();
		final List<T> list = new ArrayList<>();

		// découpe la collection en sous-listes de taille 'size'
		while (iter.hasNext()) {
			list.add(iter.next());
			if (list.size() == size) {
				output.addAll(callback.process(list));
				list.clear();
			}
		}

		// processe la dernière liste (incomplète), si nécessaire
		if (!list.isEmpty()) {
			output.addAll(callback.process(list));
			list.clear();
		}

		return output;
	}

	/**
	 * @param collection la collection d'entrée à processer
	 * @param size la taille (maximale) des portions à découper
	 * @param <T> le type des éléments contenus dans la collection
	 * @return une liste de listes des éléments initialement contenus dans la collection d'entrée
	 */
	public static <T> List<List<T>> split(Collection<T> collection, int size) {
		Assert.isTrue(size > 0);
		final int outputSize = collection.size() / size + 1;
		final List<List<T>> output = new ArrayList<>(outputSize);

		List<T> part = null;
		for (T elt : collection) {
			if (part == null) {
				part = new ArrayList<>(size);
			}
			part.add(elt);
			if (part.size() == size) {
				output.add(part);
				part = null;
			}
		}

		if (part != null && !part.isEmpty()) {
			output.add(part);
		}

		return output;
	}

	/**
	 * Méthode utilitaire qui permet d'itérer sur une liste dans l'ordre inverse de l'ordre nominal
	 * dans une construction for each...
	 */
	public static <T> Iterable<T> revertedOrder(List<T> list) {
		final ListIterator<T> iter = list.listIterator(list.size());
		final Iterator<T> revertedIterator = new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return iter.hasPrevious();
			}

			@Override
			public T next() {
				return iter.previous();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return revertedIterator;
			}
		};
	}

	/**
	 * @param first première liste présentée
	 * @param second seconde liste présentée
	 * @param <T> type des éléments des collections
	 * @return itérable sur une liste virtuelle vue comme la composition des deux listes données
	 */
	public static <T> Iterable<T> merged(List<T> first, List<T> second) {
		final Iterator<T> iterFirst = first.iterator();
		final Iterator<T> iterSecond = second.iterator();
		final Iterator<T> merged = new Iterator<T>() {
			@Override
			public boolean hasNext() {
				return iterFirst.hasNext() || iterSecond.hasNext();
			}

			@Override
			public T next() {
				return iterFirst.hasNext() ? iterFirst.next() : iterSecond.next();
			}

			@Override
			public void remove() {
				throw new UnsupportedOperationException();
			}
		};
		return new Iterable<T>() {
			@Override
			public Iterator<T> iterator() {
				return merged;
			}
		};
	}

	/**
	 * @param list une liste d'éléments
	 * @param <T> le type d'élément dans la liste
	 * @return le dernier élément de la liste (utilise l'accès direct si la liste implémente {@link RandomAccess} ou un accès par {@link ListIterator} dans le cas contraire)
	 * @throws IllegalArgumentException si la liste est nulle ou vide
	 */
	public static <T> T getLastElement(List<T> list) {
		if (list == null || list.isEmpty()) {
			throw new IllegalArgumentException("Empty list has no last element!");
		}
		if (list instanceof RandomAccess) {
			return list.get(list.size() - 1);
		}
		else {
			return list.listIterator(list.size()).previous();
		}
	}
}
