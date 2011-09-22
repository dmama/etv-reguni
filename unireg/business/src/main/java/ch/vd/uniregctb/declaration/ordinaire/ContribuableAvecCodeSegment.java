package ch.vd.uniregctb.declaration.ordinaire;

/**
 * Classe utilisée pour les données issues du fichier CSV passé en entrée du job d'import des codes segment
 */
public final class ContribuableAvecCodeSegment {

	private final long noContribuable;
	private final int codeSegment;

	public ContribuableAvecCodeSegment(long noContribuable, int codeSegment) {
		this.noContribuable = noContribuable;
		this.codeSegment = codeSegment;
	}

	public long getNoContribuable() {
		return noContribuable;
	}

	public int getCodeSegment() {
		return codeSegment;
	}

	@Override
	public String toString() {
		return String.format("%d (%d)", noContribuable, codeSegment);
	}
}
