package ch.vd.uniregctb.declaration;

/**
 * Clé à utiliser vis-à-vis du {@link ch.vd.uniregctb.common.TicketService} quand on imprime une nouvelle déclaration (= pas un duplicata, seulement
 * quand il s'agit d'une <b>nouvelle</b> déclaration) pour un tiers donné
 */
public final class DeclarationGenerationOperation {

	private final long noTiers;

	public DeclarationGenerationOperation(long noTiers) {
		this.noTiers = noTiers;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final DeclarationGenerationOperation that = (DeclarationGenerationOperation) o;
		return noTiers == that.noTiers;
	}

	@Override
	public int hashCode() {
		return (int) (noTiers ^ (noTiers >>> 32));
	}
}
