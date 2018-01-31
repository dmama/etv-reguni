package ch.vd.uniregctb.common;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

public abstract class NumeroIDEHelper {

	private static final Pattern IDE_PATTERN = Pattern.compile("(CHE|ADM)([0-9]{8})([0-9])");
	private static final Pattern PATTERN_SANS_CHIFFRE_CONTROLE = Pattern.compile("^[0-9]{8}$");
	private static final int[] MOD11_WEIGHTS = {5, 4, 3, 2, 7, 6, 5, 4};

	public static int computeControlDigit(String digits) {
		if (digits == null) {
			throw new NullPointerException("digits");
		}

		final Matcher matcher = PATTERN_SANS_CHIFFRE_CONTROLE.matcher(digits);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Invalid digits");
		}

		return computeMod11(digits);
	}

	/**
	 * Voir http://www.pgrocer.net/Cis51/mod11.html
	 * @param digits une chaîne de 8 chiffres
	 * @return le chiffre de contrôle d'après l'algo MOD11
	 */
	private static int computeMod11(String digits) {
		final char[] array = digits.toCharArray();
		if (array.length != 8) {
			throw new IllegalArgumentException("digits should be 8-digit long...");
		}

		int sum = 0;
		for (int i = 0; i < array.length ; ++ i) {
			sum += (array[i] - '0') * MOD11_WEIGHTS[i];
		}

		final int remainder = sum % 11;
		if (remainder == 0) {
			return 0;
		}

		final int ctrl = 11 - remainder;
		if (ctrl == 10) {
			throw new IllegalArgumentException("Ambiguous result");
		}
		return ctrl;
	}

	/**
	 * Normalisation du numéro IDE avant analyse resp. stockage
	 * @param valeurBrutte valeur brutte telle que saisie, avec des points, tirets et/ou espaces
	 * @return la valeur épurée des points, tirets et espaces (<code>null</code> s'il ne reste plus rien après ça)
	 */
	@Nullable
	public static String normalize(String valeurBrutte) {
		return valeurBrutte == null
				? null
				: StringUtils.trimToNull(valeurBrutte.replaceAll("[\\s\\u00a0.-]+", StringUtils.EMPTY).toUpperCase(Locale.ENGLISH));
	}

	/**
	 * Vérification qu'un numéro IDE est syntaxiquement valide (au format défini par {@link #IDE_PATTERN} avec le bon chiffre de contrôle
	 * @param ide IDE à tester
	 * @return <code>true</code> s'il est bien composé de CHE ou ADM suivi de 9 chiffres (après normalisation, voir {@link #normalize(String)}) avec un chiffre de contrôle correct
	 */
	public static boolean isValid(String ide) {
		final String normalized = normalize(ide);
		if (normalized == null) {
			// aucune information intéressante -> ce n'est pas un numéro IDE valide...
			return false;
		}
		final Matcher matcher = IDE_PATTERN.matcher(normalized);
		if (!matcher.matches()) {
			// la syntaxe même des lettres et chiffres ne va pas
			return false;
		}

		// contrôle du chiffre de contrôle, justement
		try {
			final String chiffresSansControle = matcher.group(2);
			final int chiffreControlePropose = Integer.valueOf(matcher.group(3));
			final int chiffreControleCalcule = computeMod11(chiffresSansControle);
			return chiffreControleCalcule == chiffreControlePropose;
		}
		catch (IllegalArgumentException e) {
			// chiffre de contrôle incalculable
			return false;
		}
	}
}
