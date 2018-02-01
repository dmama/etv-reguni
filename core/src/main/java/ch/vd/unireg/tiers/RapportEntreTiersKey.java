package ch.vd.uniregctb.tiers;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.type.TypeRapportEntreTiers;

public class RapportEntreTiersKey {

	public enum Source {
		/**
		 * Le rapport est vu du côté SUJET
		 */
		SUJET,

		/**
		 * Le rapport est vu du côté OBJET
		 */
		OBJET
	}

	private final TypeRapportEntreTiers type;
	private final Source source;

	public RapportEntreTiersKey(@NotNull TypeRapportEntreTiers type, @NotNull Source source) {
		this.type = type;
		this.source = source;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final RapportEntreTiersKey that = (RapportEntreTiersKey) o;
		return type == that.type && source == that.source;
	}

	@Override
	public int hashCode() {
		int result = type.hashCode();
		result = 31 * result + source.hashCode();
		return result;
	}

	public TypeRapportEntreTiers getType() {
		return type;
	}

	public Source getSource() {
		return source;
	}

	/**
	 * @return le nombre maximal de valeurs différentes existantes
	 */
	public static int maxCardinality() {
		return Source.values().length * TypeRapportEntreTiers.values().length;
	}
}
