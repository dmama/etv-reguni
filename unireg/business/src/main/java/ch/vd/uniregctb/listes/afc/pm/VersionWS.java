package ch.vd.uniregctb.listes.afc.pm;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.type.FormeJuridiqueEntreprise;
import ch.vd.uniregctb.type.MotifFor;
import ch.vd.uniregctb.type.MotifRattachement;
import ch.vd.uniregctb.type.TypeEtatEntreprise;
import ch.vd.uniregctb.type.TypeFlagEntreprise;
import ch.vd.uniregctb.xml.EnumHelper;

/**
 * Type énuméré des versions du WS (utilisée pour la traduction des valeurs énumérées exposées dans l'extraction RPT PM)
 * associées aux traductions utilisées
 */
public enum VersionWS {

	V6 {
		@Override
		public String of(@Nullable FormeJuridiqueEntreprise fj) {
			return toString(EnumHelper.coreToXMLv4(fj));
		}

		@Override
		public String of(@Nullable TypeFlagEntreprise flag) {
			return toString(EnumHelper.coreToXMLv4(flag));
		}

		@Override
		public String of(@Nullable MotifRattachement motif) {
			return toString(EnumHelper.coreToXMLv3(motif));
		}

		@Override
		public String of(@Nullable MotifFor motif) {
			return toString(EnumHelper.coreToXMLv3(motif));
		}

		@Override
		public String of(@Nullable TypeEtatEntreprise etat) {
			return toString(EnumHelper.coreToXMLv4(etat));
		}
	},

	V7 {
		@Override
		public String of(@Nullable FormeJuridiqueEntreprise fj) {
			return toString(EnumHelper.coreToXMLv5(fj));
		}

		@Override
		public String of(@Nullable TypeFlagEntreprise flag) {
			return toString(EnumHelper.coreToXMLv5(flag));
		}

		@Override
		public String of(@Nullable MotifRattachement motif) {
			return toString(EnumHelper.coreToXMLv4(motif));
		}

		@Override
		public String of(@Nullable MotifFor motif) {
			return toString(EnumHelper.coreToXMLv4(motif));
		}

		@Override
		public String of(@Nullable TypeEtatEntreprise etat) {
			return toString(EnumHelper.coreToXMLv5(etat));
		}
	};

	/**
	 * @param fj forme juridique
	 * @return chaîne de caractères à exposer
	 */
	public abstract String of(@Nullable FormeJuridiqueEntreprise fj);

	/**
	 * @param flag type de spécificité
	 * @return chaîne de caractères à exposer
	 */
	public abstract String of(@Nullable TypeFlagEntreprise flag);

	/**
	 * @param motif motif de rattachement
	 * @return chaîne de caractères à exposer
	 */
	public abstract String of(@Nullable MotifRattachement motif);

	/**
	 * @param motif motif d'ouverture ou de fermeture de for
	 * @return chaîne de caractères à exposer
	 */
	public abstract String of(@Nullable MotifFor motif);

	/**
	 * @param etat état d'entreprise
	 * @return chaîne de caractères à exposer
	 */
	public abstract String of(@Nullable TypeEtatEntreprise etat);

	static <T extends Enum<T>> String toString(T value) {
		return value != null ? value.name() : null;
	}
}
