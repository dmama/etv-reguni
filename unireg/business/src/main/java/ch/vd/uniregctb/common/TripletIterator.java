package ch.vd.uniregctb.common;

import java.util.Iterator;

import ch.vd.registre.base.utils.NotImplementedException;

/**
 * Itérateur qui retourne les éléments <i>précédents</i> et <i>suivants</i> <b>en plus</b> de l'élément <i>courant</i>.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class TripletIterator<E> implements Iterator<Triplet<E>> {

	private final Iterator<E> iter;
	private Triplet<E> next;

	/**
	 * Itérateur qui retourne les éléments <i>précédents</i> et <i>suivants</i> <b>en plus</b> de l'élément <i>courant</i>.
	 *
	 * @param iter un itérateur normal sur lequel se baser.
	 */
	public TripletIterator(Iterator<E> iter) {
		this.iter = iter;
		this.next = buildNext();
	}

	private Triplet<E> buildNext() {
		E p = null;
		E c = null;
		E n = null;
		if (this.next == null) {
			// premier appel -> initialisation
			if (iter.hasNext()) {
				c = iter.next();
			}
			if (iter.hasNext()) {
				n = iter.next();
			}
		}
		else {
			// appel second et suivants -> on décale les éléments
			p = this.next.current;
			c = this.next.next;
			if (iter.hasNext()) {
				n = iter.next();
			}
		}
		if (c == null) {
			return null;
		}
		else {
			return new Triplet<E>(p, c, n);
		}
	}

	public boolean hasNext() {
		return next != null;
	}

	public Triplet<E> next() {
		Triplet<E> result = next;
		next = buildNext();
		return result;
	}

	public void remove() {
		throw new NotImplementedException();
	}
}
