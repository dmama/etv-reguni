package ch.vd.uniregctb.tiers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Quelques points d'entrée pratiques autour des allègements fiscaux
 */
public abstract class AllegementFiscalHelper {

	/**
	 * Clé générable depuis un allègement fiscal pour déterminer s'il a le droit de coexister avec un autre
	 * (deux allègements qui aboutissent à la même clé n'ont pas ce droit)
	 */
	public static class OverlappingKey {

		private final AllegementFiscal.TypeImpot typeImpot;
		private final AllegementFiscal.TypeCollectivite typeCollectivite;
		private final Integer noOfsCommune;

		private OverlappingKey(AllegementFiscal.TypeImpot typeImpot, AllegementFiscal.TypeCollectivite typeCollectivite, @Nullable Integer noOfsCommune) {
			this.typeImpot = typeImpot;
			this.typeCollectivite = typeCollectivite;
			this.noOfsCommune = (typeCollectivite == AllegementFiscal.TypeCollectivite.COMMUNE ? noOfsCommune : null);
		}

		/**
		 * Constructeur officiel depuis un allègement existant
		 * @param allegementFiscal un allègement fiscal
		 * @return la clé correspondante
		 */
		@NotNull
		public static OverlappingKey valueOf(AllegementFiscal allegementFiscal) {
			switch (allegementFiscal.getTypeCollectivite()) {
			case CANTON:
				return cantonal(allegementFiscal.getTypeImpot());
			case COMMUNE:
				return communal(allegementFiscal.getTypeImpot(), ((AllegementFiscalCommune) allegementFiscal).getNoOfsCommune());
			case CONFEDERATION:
				return federal(allegementFiscal.getTypeImpot());
			default:
				throw new IllegalArgumentException("Allegement fiscal de type " + allegementFiscal.getTypeCollectivite() + " non supporté!");
			}
		}

		@NotNull
		public static OverlappingKey cantonal(AllegementFiscal.TypeImpot typeImpot) {
			return new OverlappingKey(typeImpot, AllegementFiscal.TypeCollectivite.CANTON, null);
		}

		@NotNull
		public static OverlappingKey federal(AllegementFiscal.TypeImpot typeImpot) {
			return new OverlappingKey(typeImpot, AllegementFiscal.TypeCollectivite.CONFEDERATION, null);
		}

		@NotNull
		public static OverlappingKey communal(AllegementFiscal.TypeImpot typeImpot, @Nullable Integer noOfsCommune) {
			return new OverlappingKey(typeImpot, AllegementFiscal.TypeCollectivite.COMMUNE, noOfsCommune);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			final OverlappingKey that = (OverlappingKey) o;

			if (typeImpot != that.typeImpot) return false;
			if (typeCollectivite != that.typeCollectivite) return false;
			return !(noOfsCommune != null ? !noOfsCommune.equals(that.noOfsCommune) : that.noOfsCommune != null);
		}

		@Override
		public int hashCode() {
			int result = typeImpot != null ? typeImpot.hashCode() : 0;
			result = 31 * result + (typeCollectivite != null ? typeCollectivite.hashCode() : 0);
			result = 31 * result + (noOfsCommune != null ? noOfsCommune.hashCode() : 0);
			return result;
		}

		@Override
		public String toString() {
			if (typeImpot == null && typeCollectivite == null) {
				return "allègement universel";
			}

			final StringBuilder b = new StringBuilder("allègement");
			if (typeImpot != null) {
				b.append(" ").append(typeImpot);
			}
			if (typeCollectivite != null) {
				b.append(" ").append(typeCollectivite);
				if (noOfsCommune != null) {
					b.append(" (").append(noOfsCommune).append(")");
				}
			}
			return b.toString();
		}
	}
}
