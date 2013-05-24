package ch.vd.uniregctb.cache;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.emory.mathcs.backport.java.util.Collections;

/**
 * Ensemble de méthodes disponibles pour la gestion des données en cache
 */
public abstract class CacheHelper {

	private static final char CR = '\n';

	/**
	 * @param keys une collection de clés de cache
	 * @return une chaîne de caractères, avec une ligne par clé, représentant cette liste (les clé sont triées par ordre alphabétique de leur représentation {@link Object#toString()})
	 */
	public static String dumpKeys(Collection<Object> keys) {
		final List<String> keyStrings = new ArrayList<>(keys.size());
		for (Object key : keys) {
			if (key != null) {
				keyStrings.add(key.toString());
			}
		}
		Collections.sort(keyStrings);
		final StringBuilder b = new StringBuilder();
		for (String str : keyStrings) {
			b.append(" * ").append(str).append(CR);
		}
		return b.toString();
	}
}
