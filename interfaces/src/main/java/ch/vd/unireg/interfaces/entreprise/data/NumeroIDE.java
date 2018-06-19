package ch.vd.unireg.interfaces.entreprise.data;

import java.io.Serializable;
import java.util.regex.Pattern;

/**
 * Classe de modélisation du numéro IDE comme un String CHExxxxxxxxx avec validation basique.
 *
 * @author Raphaël Marmier, 2016-08-24, <raphael.marmier@vd.ch>
 */
public class NumeroIDE implements Serializable {

	private static final long serialVersionUID = -1002453602623340745L;

	private static final String PREFIX = "CHE";
	private static final Pattern ONLY_DIGITS = Pattern.compile("^[0-9]+$");

	private final String che9chiffres;

	/**
	 *
	 * @param che9chiffres chaîne de caractères commençant par CHE suivie de 9 chiffres du numéro IDE.
	 */
	public NumeroIDE(String che9chiffres) {
		if (che9chiffres.startsWith(PREFIX)) {
			this.che9chiffres = validate(che9chiffres);
		} else {
			this.che9chiffres = validate(PREFIX + che9chiffres);
		}
	}

	public static NumeroIDE valueOf(int brute) {
		return new NumeroIDE(PREFIX + String.valueOf(brute));
	}

	private String validate(String valeur) {
		final String digits = valeur.substring(3);
		if (digits.length() == 9
				&& ONLY_DIGITS.matcher(digits).matches()) {
			return valeur;
		}
		throw new IllegalArgumentException(String.format("%s n'est pas un numéro IDE préfixé valide!", valeur));
	}

	/**
	 * @return la valeur en format CHExxxxxxxxx
	 */
	public String getValeur() {
		return che9chiffres;
	}

	public int getValeurBrute() {
		final String digits = che9chiffres.substring(3);
		return Integer.parseInt(digits);
	}

	public String toString() {
		return PREFIX + "-" + che9chiffres.substring(3, 6) + "." + che9chiffres.substring(6, 9) + "." + che9chiffres.substring(9, 12);
	}
}
