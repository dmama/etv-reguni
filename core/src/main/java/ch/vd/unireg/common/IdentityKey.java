package ch.vd.unireg.common;

public final class IdentityKey<T> {

	private final T elt;

	public IdentityKey(T elt) {
		this.elt = elt;
	}

	public T getElt() {
		return elt;
	}

	@Override
	public boolean equals(Object o) {
		return (o instanceof IdentityKey && ((IdentityKey)o).elt == elt);
	}

	@Override
	public int hashCode() {
		return System.identityHashCode(elt);
	}
}
