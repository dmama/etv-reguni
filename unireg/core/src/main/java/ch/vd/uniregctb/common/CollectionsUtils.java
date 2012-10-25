package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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

		List<O> output = new ArrayList<O>();

		Assert.isTrue(size > 0);
		final Iterator<T> iter = collection.iterator();
		final List<T> list = new ArrayList<T>();

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
		final List<List<T>> output = new ArrayList<List<T>>(outputSize);

		List<T> part = null;
		for (T elt : collection) {
			if (part == null) {
				part = new ArrayList<T>(size);
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
}
