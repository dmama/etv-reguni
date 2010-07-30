package ch.vd.uniregctb.common;

/**
 * Triplet d'élément retourné par la classe {@link #TripletIterator}.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Triplet<E> {
	public final E previous;
	public final E current;
	public final E next;

	public Triplet(E previous, E current, E next) {
		this.previous = previous;
		this.current = current;
		this.next = next;
	}
}