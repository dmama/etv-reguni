package ch.vd.uniregctb.declaration.ordinaire;

public final class ContribuableAvecImmeuble {

	private final long numeroContribuable;
	private final int nombreImmeubles;

	public ContribuableAvecImmeuble(long numeroContribuable, int nombreImmeubles) {
		this.numeroContribuable = numeroContribuable;
		this.nombreImmeubles = nombreImmeubles;
	}

	public long getNumeroContribuable() {
		return numeroContribuable;
	}

	public int getNombreImmeubles() {
		return nombreImmeubles;
	}

	@Override
	public String toString() {
		return String.format("%d (%d)", numeroContribuable, nombreImmeubles);
	}
}
