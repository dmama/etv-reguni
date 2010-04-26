package ch.vd.uniregctb.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.context.support.ApplicationObjectSupport;

public class CommonMapHelper extends ApplicationObjectSupport {

	/**
	 * Transforme une énumération en map indexée par enum et dont les valeurs sont les descriptions pour l'utilisateur
	 *
	 * @param keyPrefix
	 *            le préfixe de la ressource utilisée pour récupérer les descriptions des enums
	 * @param clazz
	 *            la classe de l'énumération
	 * @param ignored
	 *            une liste optionnelle des enums à ignorer
	 * @return une map
	 */
	protected <T extends Enum<T>> Map<T, String> initMapEnum(String keyPrefix, Class<T> clazz, T... ignored) {
		final Map<String, T> mapTmp = new HashMap<String, T>();
		final Map<T, String> map = new LinkedHashMap<T, String>();
		final T[] constants = clazz.getEnumConstants();
		final Set<T> ignor = (ignored == null ? null : new HashSet<T>(Arrays.asList(ignored)));

		for (T c : constants) {
			if (ignor == null || !ignor.contains(c)) {
				final String nom = this.getMessageSourceAccessor().getMessage(keyPrefix + c);
				mapTmp.put(nom, c);
			}
		}

		final List<String> nomList = new ArrayList<String>(mapTmp.keySet());
		Collections.sort(nomList);
		for (String aNomList : nomList) {
			final T c = mapTmp.get(aNomList);
			map.put(c, aNomList);
		}

		return Collections.unmodifiableMap(map);
	}

}
