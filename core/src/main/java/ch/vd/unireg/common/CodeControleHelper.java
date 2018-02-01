package ch.vd.unireg.common;

public abstract class CodeControleHelper {

	// Toutes les lettres, sauf le 'O' qui peut être confondu avec le '0'.
	// [SIFISC-4453] ... et le 'I' qui peut être confondu avec le '1'
	private static final char CODE_LETTERS[] = {'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'J', 'K', 'L', 'M', 'N', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'};

	/**
	 * Génère un code de contrôle pour le retour des déclarations d'impôt sous forme électronique. Ce code de contrôle est une string de 6 caractères composée d'une lettre suivie de 5 chiffres pris au
	 * hazard (voir spécification dans SIFISC-1368).
	 * <p/>
	 * <b>Exemples</b>:
	 * <ul>
	 *     <li>B62116</li>
	 *     <li>U94624</li>
	 *     <li>H57736</li>
	 *     <li>E93590</li>
	 *     <li>V34032</li>
	 *     <li>N43118</li>
	 *     <li>B98052</li>
	 *     <li>S67086</li>
	 *     <li>...</li>
	 * </ul>
	 *
	 * @return un code de contrôle composé d'une lettre et de cinq chiffres
	 */
	public static String generateCodeControleUneLettreCinqChiffres() {
		final int letter_index = (int) (CODE_LETTERS.length * Math.random());
		final int number = (int) (100000 * Math.random());
		return String.format("%s%05d", CODE_LETTERS[letter_index], number);
	}
}
