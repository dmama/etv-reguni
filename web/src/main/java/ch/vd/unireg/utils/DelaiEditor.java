package ch.vd.unireg.utils;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import ch.vd.unireg.type.delai.Delai;

/**
 * Editeur pour les classes {@link ch.vd.unireg.type.delai.Delai}.
 */
public class DelaiEditor extends PropertyEditorSupport {

	private final boolean allowEmpty;
	private final boolean silentParsingError;
	private final Format format;

	public enum Format {
		/**
		 * e.g 2M + 75D
		 */
		TECHNICAL {
			@Override
			public Delai parse(@NotNull String text) {
				return Delai.fromString(text);
			}

			@Override
			public String toString(@NotNull Delai delai) {
				return delai.toString();
			}
		},
		/**
		 * e.g. 2 mois + 75 jours
		 */
		DISPLAY {
			@Override
			public Delai parse(@NotNull String text) {
				return Delai.fromString(text.replaceAll("\\s*jours", "D").replaceAll("\\s*mois", "M"));
			}

			@Override
			public String toString(@NotNull Delai delai) {
				return delai.toString().replaceAll("D", " jours").replaceAll("M", " mois");
			}
		};

		public abstract Delai parse(@NotNull String text);
		public abstract String toString(@NotNull Delai delai);
	}


	/**
	 * @param allowEmpty         <b>vrai</b> si la donnée peut être nulle; <b>faux</b> pour qu'une donnée nulle lève une erreur.
	 * @param silentParsingError <b>vrai</b> pour qu'une donnée malformée soit interprétée comme nulle; <b>faux</b> pour qu'une erreur soit levée.
	 * @param format             le format à utiliser
	 */
	public DelaiEditor(boolean allowEmpty, boolean silentParsingError, @NotNull Format format) {
		this.allowEmpty = allowEmpty;
		this.silentParsingError = silentParsingError;
		this.format = format;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {

		if (this.allowEmpty && StringUtils.isBlank(text)) {
			// treat empty String as null value
			setValue(null);
		}
		else {
			try {
				final Delai delai = format.parse(text);
				setValue(delai);
			}
			catch (IllegalArgumentException e) {
				if (silentParsingError) {
					setValue(null);
				}
				else {
					throw e;
				}
			}
		}
	}

	@Override
	public String getAsText() {
		final Delai value = (Delai) getValue();
		return (value != null ? format.toString(value) : StringUtils.EMPTY);
	}
}
