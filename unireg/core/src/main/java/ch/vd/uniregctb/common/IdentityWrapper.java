package ch.vd.uniregctb.common;

public final class IdentityWrapper<T> {

	private final T elt;

	public IdentityWrapper(T elt) {
		this.elt = elt;
	}

	public T getElt() {
		return elt;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof IdentityWrapper && ((IdentityWrapper)o).elt == elt);
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(elt);
	}
}
