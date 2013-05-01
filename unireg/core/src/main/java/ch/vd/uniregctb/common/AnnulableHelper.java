package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

/**
 * Classe utilitaire autour des éléments {@link Annulable}
 */
public abstract class AnnulableHelper {

	/**
	 * @param col une collection d'éléments annulables
	 * @param <T> le type des éléments annuables
	 * @return une liste des seuls éléments non-annulés de la liste de départ (dans l'ordre de l'itérateur canonique de la collection fournie)
	 */
	@NotNull
	public static <T extends Annulable> List<T> sansElementsAnnules(Collection<T> col) {
		if (col == null || col.isEmpty()) {
			return Collections.emptyList();
		}
		return sansElementsAnnules(col.iterator(), col.size());
	}

	@NotNull
	private static <T extends Annulable> List<T> sansElementsAnnules(Iterator<T> iterator, int size) {
		final List<T> res = new ArrayList<>(size);
		while (iterator.hasNext()) {
			final T elt = iterator.next();
			if (!elt.isAnnule()) {
				res.add(elt);
			}
		}
		return res.size() == 0 ? Collections.<T>emptyList() : res;
	}
}