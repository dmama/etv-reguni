package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.RandomAccess;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.utils.Assert;

public abstract class CollectionsUtils {

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

	/**
	 * @param col collection à transformer en chaîne de caractères
	 * @param renderer le {@link ch.vd.uniregctb.common.StringRenderer} à utiliser pour les éléments de la collection
	 * @param separator le séparateur à placer entre la représentation de chacun des éléments de la collection
	 * @param emptyCollectionString la chaîne de caractère à afficher dans le cas d'une collection nulle ou vide
	 * @param <T> type des éléments de la collection
	 * @return une chaîne de caractère qui énumère les éléments de la collection, séparés par le séparateur donné
	 */
	public static <T> String toString(Collection<T> col, StringRenderer<? super T> renderer, String separator, String emptyCollectionString) {
		if (col == null || col.isEmpty()) {
			return emptyCollectionString;
		}
		final StringBuilder b = new StringBuilder();
		for (T elt : col) {
			if (b.length() > 0) {
				b.append(separator);
			}
			b.append(renderer.toString(elt));
		}
		return b.toString();
	}

	/**
	 * @param col collection à transformer en chaîne de caractères
	 * @param renderer le {@link ch.vd.uniregctb.common.StringRenderer} à utiliser pour les éléments de la collection
	 * @param separator le séparateur à placer entre la représentation de chacun des éléments de la collection
	 * @param <T> type des éléments de la collection
	 * @return une chaîne de caractère qui énumère les éléments de la collection, séparés par le séparateur donné (en cas de collection nulle ou vide, renvoie une chaîne vide)
	 */
	public static <T> String toString(Collection<T> col, StringRenderer<? super T> renderer, String separator) {
		return toString(col, renderer, separator, StringUtils.EMPTY);
	}

	private static final StringRenderer<String> STRING_TRIMMER = new StringRenderer<String>() {
		@Override
		public String toString(String str) {
			return StringUtils.trimToEmpty(str);
		}
	};

	public static String concat(List<String> list, String separator) {
		return toString(list, STRING_TRIMMER, separator);
	}
}
