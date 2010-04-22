package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Collections d'utilitaire pour la manipulation des listes.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public abstract class ListUtils {

	/**
	 * Découpe une collection en plusieurs sous-listes de tailles fixes.
	 *
	 * @param input la liste d'entrée
	 * @param size  la taille maximale des sous-listes
	 * @return une liste contenant <i>n</i> sous-listes.
	 */
	public static <T> List<List<T>> split(Collection<T> input, int size) {
		final List<List<T>> list = new ArrayList<List<T>>((input.size() / size) + 1);
		List<T> current = null;
		for (T t : input) {
			if (current == null) {
				current = new ArrayList<T>(size);
			}
			current.add(t);
			if (current.size() == size) {
				list.add(current);
				current = null;
			}
		}
		if (current != null) {
			list.add(current);
		}
		return list;
	}

}
