package ch.vd.uniregctb.common;

import java.util.Collection;
import java.util.Iterator;

import ch.vd.registre.base.utils.NotImplementedException;

/**
 * Un itérateur très gentil qui permet:
 * <ul>
 * <li>de détecter la progression de l'itération pourcents par pourcent</li>
 * <li>de savoir si l'itérateur est sur le premier élément</li>
 * <li>de savoir si l'itérateur est sur le dernier élément</li>
 * </ul>
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class GentilIterator<T> implements Iterator<T> {

	private int previous_percent;
	private int current_percent;
	private int i;
	private final int size;
	private final Iterator<T> iter;

	public GentilIterator(Collection<T> collection) {
		this(collection.iterator(), collection.size());
	}

	public GentilIterator(Iterator<T> iterator, int size) {
		this.size = size;
		this.previous_percent = -1;
		this.current_percent = -1;
		this.i = -1;
		this.iter = iterator;
	}

	@Override
	public boolean hasNext() {
		return iter.hasNext();
	}

	@Override
	public T next() {
		previous_percent = current_percent;
		current_percent = (++i * 100) / size;
		return iter.next();
	}

	/**
	 * @return le pourcentage de progression
	 */
	public int getPercent() {
		return current_percent;
	}

	/**
	 * @return <b>vrai</b> si l'itérateur est sur un "nouveau" pourcent
	 */
	public boolean isAtNewPercent() {
		return current_percent != previous_percent;
	}

	/**
	 * @return <b>vrai</b> si l'itérateur est sur le premier element
	 */
	public boolean isFirst() {
		return i == 0;
	}

	/**
	 * @return <b>vrai</b> si l'itérateur est sur le dernier element
	 */
	public boolean isLast() {
		return i >= 0 && !hasNext();
	}

	@Override
	public void remove() {
		throw new NotImplementedException();
	}
}
