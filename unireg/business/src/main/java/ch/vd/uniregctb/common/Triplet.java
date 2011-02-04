package ch.vd.uniregctb.common;

/**
 * Triplet d'élément retourné par la classe {@link #TripletIterator}.
 *
 * @author Manuel Siggen <manuel.siggen@vd.ch>
 */
public class Triplet<E> {
	public final E previousprevious;
	public final E previous;
	public final E current;
	public final E next;
	public final E nextnext;

	public Triplet(E previousprevious, E previous, E current, E next, E nextnext) {
		this.previousprevious = previousprevious;
		this.previous = previous;
		this.current = current;
		this.next = next;
		this.nextnext = nextnext;
	}
}