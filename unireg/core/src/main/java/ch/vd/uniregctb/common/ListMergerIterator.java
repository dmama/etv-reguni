package ch.vd.uniregctb.common;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * Itérateur qui itère sur les éléments de deux listes (supposées triées selon le comparateur fourni) en utilisant
 * toujours le "plus petit" élément (au sens du comparateur fourni) venant de l'une ou l'autre des listes
 * (en cas d'égalité, c'est la première liste qui passe d'abord)
 * @param <T>
 */
public final class ListMergerIterator<T> implements Iterator<T> {

	private final Comparator<T> comparator;
	private final Iterator<T> iterator1;
	private final Iterator<T> iterator2;
	private T element1 = null;
	private T element2 = null;
	private boolean element1Extrait = false;
	private boolean element2Extrait = false;

	public ListMergerIterator(List<T> liste1, List<T> liste2, Comparator<T> comparator) {
		this.comparator = comparator;
		this.iterator1 = liste1.iterator();
		this.iterator2 = liste2.iterator();
	}

	@Override
	public boolean hasNext() {
		return iterator1.hasNext() || iterator2.hasNext() || element1Extrait || element2Extrait;
	}

	@Override
	public T next() {
		if (!iterator1.hasNext() && !element1Extrait) {
			// cas facile où la liste1 est déja complètement passée
			if (element2Extrait) {
				element2Extrait = false;
				return element2;
			}
			else {
				return iterator2.next();
			}
		}
		else if (!iterator2.hasNext() && !element2Extrait) {
			// cas facile où la liste2 est déjà complètement passée
			if (element1Extrait) {
				element1Extrait = false;
				return element1;
			}
			else {
				return iterator1.next();
			}
		}
		else {
			// cas où il faut choisir le plus petit des deux éléments qui se présentent...
			final T candidat1 = (element1Extrait ? element1 : iterator1.next());
			final T candidat2 = (element2Extrait ? element2 : iterator2.next());
			final int comparison = comparator.compare(candidat1, candidat2);
			if (comparison <= 0) {
				element1Extrait = false;
				element2Extrait = true;
				element2 = candidat2;
				return candidat1;
			}
			else {
				element1Extrait = true;
				element2Extrait = false;
				element1 = candidat1;
				return candidat2;
			}
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}
}
