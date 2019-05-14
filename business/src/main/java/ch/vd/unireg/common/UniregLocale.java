package ch.vd.unireg.common;

import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Configuration de la locale pour Unireg
 */
public abstract class UniregLocale {

	/**
	 * Locale par défaut
	 */
	public static final Locale LOCALE = new Locale("fr", "CH");

	/**
	 * Configuration des symboles décimaux.
	 */
	public static final DecimalFormatSymbols SYMBOLS;
	static {
		// Depuis le jdk 11, la locale fr-CH définit la virgule comme séparateur décimal et
		// l'espace comme séparateur de groupe -> on force le point et l'apostrophe
		// (voir https://bugs.openjdk.java.net/browse/JDK-8211262)
		SYMBOLS = new DecimalFormatSymbols(LOCALE);
		SYMBOLS.setDecimalSeparator('.');
		SYMBOLS.setGroupingSeparator('\'');
	}

	private UniregLocale() {
	}
}
