package ch.vd.unireg.interfaces.entreprise.data.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BuilderHelper {

	/**
	 * Ajoute une valeur à la liste correspondant à une clé de la map.
	 *
	 * @param map Une map de listes. Si null, une nouvelle map sera créé en place.
	 * @param key Une clé pour une liste de la map
	 * @param value La valeur à insérer
	 * @param <K> Le type de la clé
	 * @param <V> Le type de la valeur
	 * @return La map de liste modifiée ou créée dans l'opération
	 */
	public static <K,V> Map<K, List<V>> addValueToMapOfList(@Nullable Map<K, List<V>> map,
	                                             @NotNull final K key,
	                                             @NotNull final V value) {
		if (map == null) {
			map = new HashMap<>();
		}
		final List<V> l = map.computeIfAbsent(key, k -> new ArrayList<>());
		l.add(value);
		return map;
	}

	/**
	 * Ajoute une valeur à la liste.
	 *
	 * @param list Une liste. Si null, une nouvelle liste sera créé en place.
	 * @param value La valeur à insérer
	 * @param <T> Le type de la valeur
	 * @return La liste modifiée ou créée dans l'opération
	 */
	public static <T> List<T> addValueToList(@Nullable List<T> list, @NotNull T value) {
		if (list == null) {
			list = new ArrayList<>();
		}
		list.add(value);
		return list;
	}

	/**
	 * Ajoute une valeur à la map.
	 *
	 * @param map Une map. Si null, une nouvelle map sera créé en place.
	 * @param key La clé représentant la valeur dans la map
	 * @param value La valeur à insérer
	 * @param <K> Le type de la clé
	 * @param <V> Le type de la valeur
	 * @return La liste modifiée ou créée dans l'opération
	 */
	public static <K, V> Map<K, V> addValueToMap(@Nullable Map<K, V> map, @NotNull K key, @NotNull V value) {
		if (map == null) {
			map = new HashMap<>();
		}
		map.put(key, value);
		return map;
	}
}
