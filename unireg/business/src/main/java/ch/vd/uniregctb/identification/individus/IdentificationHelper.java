package ch.vd.uniregctb.identification.individus;

import java.util.regex.Pattern;

public abstract class IdentificationHelper {

	public static final Pattern DOUBLON_PATTERN = Pattern.compile("[- /]*doublon$", Pattern.CASE_INSENSITIVE);
	public static final Pattern PONCTUATION_PATTERN = Pattern.compile("[-']+");

	public static String removeDoublonSuffixe(String nom) {
		return DOUBLON_PATTERN.matcher(nom).replaceAll("");
	}

	public static String removePonctuation(String nom) {
		return PONCTUATION_PATTERN.matcher(nom).replaceAll(" ");
	}
}
