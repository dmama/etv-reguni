package ch.vd.uniregctb.declaration.ordinaire;

public final class ContribuableAvecImmeuble {
	long numeroContribuable;
	int nombreImmeuble;

	public ContribuableAvecImmeuble(long numeroContribuable, int nombreImmeuble) {
		this.numeroContribuable = numeroContribuable;
		this.nombreImmeuble = nombreImmeuble;
	}

	public long getNumeroContribuable() {
		return numeroContribuable;
	}

	public void setNumeroContribuable(long numeroContribuable) {
		this.numeroContribuable = numeroContribuable;
	}

	public int getNombreImmeuble() {
		return nombreImmeuble;
	}

	public void setNombreImmeuble(int nombreImmeuble) {
		this.nombreImmeuble = nombreImmeuble;
	}

	@Override
	public String toString() {
		return String.format("%d (%d)", numeroContribuable, nombreImmeuble);
	}
}
