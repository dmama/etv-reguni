package ch.vd.uniregctb.common;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Comparateur un peu intelligent qui est capable de comparer des chaînes de caractères
 * sans tenir comptes des accents et en étant capable de décoder les strings sources
 */
public final class StringComparator implements Comparator<String> {

	private final boolean accentSensitive;
	private final boolean caseSensitive;
	private final boolean nullFirst;
	private final Decoder decoder;

	/**
	 * Correspondance entre les caractères accentués et les non-accentués
	 * (avec gestion des multi-caractères)
	 */
	private static final Map<Character, String> mapping = initMapping();

	/**
	 * Décodeur passé au comparateur qui permet de modifier la string
	 * avant de la passer dans la moulinette du comparateur
	 */
	public static interface Decoder {
		String decode(String source);
	}

	/**
	 * Constructeur
	 * @param accentSensitive <code>true</code> si la comparaison doit être sensible aux accents (ê != e, par exemple), <code>false</code> dans le cas contraire
	 * @param caseSensitive <code>true</code> si la comparaison doit être sensible à la casse, <code>false</code> dans le cas contraire
	 * @param nullFirst <code>true</code> si la comparaison doit mettre les éléments <code>null</code> au début de la liste, <code>false</code> dans le cas contraire
	 * @param decoder décodeur (optionnel) pour permettre un décodage on-the-fly des chaînes de caractères avant la comparaison
	 */
	public StringComparator(boolean accentSensitive, boolean caseSensitive, boolean nullFirst, Decoder decoder) {
		this.accentSensitive = accentSensitive;
		this.caseSensitive = caseSensitive;
		this.nullFirst = nullFirst;
		this.decoder = decoder;
	}

	public int compare(String o1, String o2) {

		// le test facile et rapide
		//noinspection StringEquality
		if (o1 == o2) {
			return 0;
		}

		if (decoder != null) {
			o1 = decoder.decode(o1);
			o2 = decoder.decode(o2);
		}

		if (o1 == null) {
			return nullFirst ? -1 : 1;
		}
		else if (o2 == null) {
			return nullFirst ? 1 : -1;
		}
		else {
			if (!accentSensitive) {
				o1 = removeAccents(o1);
				o2 = removeAccents(o2);
			}

			if (caseSensitive) {
				return o1.compareTo(o2);
			}
			else {
				return o1.compareToIgnoreCase(o2);
			}
		}
	}

	public static String removeAccents(String src) {

		if (src == null) {
			return null;
		}

		final int length = src.length();

		// vérification s'il y a des accents ou pas
		boolean hasAccent = false;
		for (int i = 0 ; i < length ; ++ i) {
			final String s = mapping.get(src.charAt(i));
			if (s != null) {
				hasAccent = true;
				break;
			}
		}

		// on évite de faire des copies de strings pour rien
		if (!hasAccent) {
			return src;
		}

		// autrement, on fait une copie et on supprime les accents
		final StringBuilder b = new StringBuilder(src.length() * 2);
		for (Character c : src.toCharArray()) {
			final String s = mapping.get(c);
			if (s == null) {
				b.append(c);
			}
			else {
				b.append(s);
			}
		}
		return b.toString();
	}

	private static Map<Character, String> initMapping() {
		final Map<Character, String> map = new HashMap<Character, String>();
		fillMap(map, "a", 'á', 'à', 'â', 'ä', 'ã', 'å');
		fillMap(map, "A", 'Á', 'À', 'Â', 'Ä', 'Ã', 'Å');
		fillMap(map, "ae", 'æ');
		fillMap(map, "AE", 'Æ');
		fillMap(map, "c", 'ç');
		fillMap(map, "C", 'Ç');
		fillMap(map, "d", 'ð');
		fillMap(map, "D", 'Ð');
		fillMap(map, "e", 'é', 'è', 'ê', 'ë', 'ẽ');
		fillMap(map, "E", 'É', 'È', 'Ê', 'Ë', 'Ẽ');
		fillMap(map, "i", 'í', 'ì', 'î', 'ï', 'ĩ');
		fillMap(map, "I", 'Í', 'Ì', 'Î', 'Ï', 'Ĩ');
		fillMap(map, "l", 'ł');
		fillMap(map, "L", 'Ł');
		fillMap(map, "n", 'ñ');
		fillMap(map, "N", 'Ñ');
		fillMap(map, "o", 'ó', 'ò', 'ô', 'ö', 'õ', 'ø');
		fillMap(map, "O", 'Ó', 'Ò', 'Ô', 'Ö', 'Õ', 'Ø');
		fillMap(map, "oe", 'œ');
		fillMap(map, "OE", 'Œ');
		fillMap(map, "p", 'Þ');
		fillMap(map, "P", 'þ');
		fillMap(map, "s", 'š');
		fillMap(map, "S", 'Š');
		fillMap(map, "ss", 'ß');
		fillMap(map, "u", 'ú', 'ù', 'û', 'ü', 'ũ');
		fillMap(map, "U", 'Ú', 'Ù', 'Û', 'Ü', 'Ũ');
		fillMap(map, "y", 'ý', 'ỳ', 'ŷ', 'ÿ', 'ỹ');
		fillMap(map, "Y", 'Ý', 'Ỳ', 'Ŷ', 'Ÿ', 'Ỹ');
		fillMap(map, "z", 'ž');
		fillMap(map, "Z", 'Ž');
		return map;
	}

	private static void fillMap(Map<Character, String> map, String destination, char... sources) {
		for (char src : sources) {
			map.put(src, destination);
		}
	}

	public static String toLowerCaseWithoutAccent(String str) {
		if (str == null) {
			return "";
		}
		else {
			return removeAccents(str).toLowerCase();
		}
	}
}
