package ch.vd.unireg.common;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.RandomAccess;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CollectionsUtils {

	public interface SplitCallback<T, O> {
		List<O> process(List<T> list);
	}

	@NotNull
	public static <T> List<T> union(@NotNull Collection<T> left, @NotNull Collection<T> right) {
		final List<T> union = new ArrayList<>(left.size() + right.size());
		union.addAll(left);
		union.addAll(right);
		return union;
	}

	/**
	 * Cette méthode découpe une collection d'éléments en plusieurs sous-listes de taille déterminée, et appelle un callback sur chacune des sous-listes.
	 *
	 * @param collection la collection d'entrée à processer
	 * @param size       la taille (maximale) des sous-listes
	 * @param callback   la méthode de callback appelée sur chacune des sous-listes
	 * @param <T>        le type des éléments contenus dans la collection
	 * @return une collection contenant tous les éléments retournés par les appels aux méthodes 'process' des callbacks.
	 */
	public static <T, O> List<O> splitAndProcess(Collection<T> collection, int size, SplitCallback<T, O> callback) {

		List<O> output = new ArrayList<>();

		if (size <= 0) {
			throw new IllegalArgumentException();
		}
		final Iterator<T> iter = collection.iterator();
		final List<T> list = new ArrayList<>();

		// découpe la collection en sous-listes de taille 'size'
		while (iter.hasNext()) {
			list.add(iter.next());
			if (list.size() == size) {
				output.addAll(callback.process(list));
				list.clear();
			}
		}

		// processe la dernière liste (incomplète), si nécessaire
		if (!list.isEmpty()) {
			output.addAll(callback.process(list));
			list.clear();
		}

		return output;
	}

	/**
	 * @param collection la collection d'entrée à processer
	 * @param size       la taille (maximale) des portions à découper
	 * @param <T>        le type des éléments contenus dans la collection
	 * @return une liste de listes des éléments initialement contenus dans la collection d'entrée
	 */
	public static <T> List<List<T>> split(Collection<T> collection, int size) {
		if (size <= 0) {
			throw new IllegalArgumentException();
		}
		final int outputSize = collection.size() / size + 1;
		final List<List<T>> output = new ArrayList<>(outputSize);

		List<T> part = null;
		for (T elt : collection) {
			if (part == null) {
				part = new ArrayList<>(size);
			}
			part.add(elt);
			if (part.size() == size) {
				output.add(part);
				part = null;
			}
		}

		if (part != null && !part.isEmpty()) {
			output.add(part);
		}

		return output;
	}

	/**
	 * Méthode utilitaire qui permet d'itérer sur une liste dans l'ordre inverse de l'ordre nominal
	 * dans une construction for each...
	 */
	public static <T> Iterable<T> revertedOrder(List<T> list) {
		return () -> {
			final ListIterator<T> iter = list.listIterator(list.size());
			return new Iterator<T>() {
				@Override
				public boolean hasNext() {
					return iter.hasPrevious();
				}

				@Override
				public T next() {
					return iter.previous();
				}
			};
		};
	}

	/**
	 * @param first  première liste présentée
	 * @param second seconde liste présentée
	 * @param <T>    type commun des éléments des collections
	 * @return itérable sur une liste virtuelle vue comme la composition des deux listes données
	 */
	public static <T> Iterable<T> merged(Iterable<? extends T> first, Iterable<? extends T> second) {
		return () -> {
			final Iterator<? extends T> iterFirst = first.iterator();
			final Iterator<? extends T> iterSecond = second.iterator();
			return new Iterator<T>() {
				@Override
				public boolean hasNext() {
					return iterFirst.hasNext() || iterSecond.hasNext();
				}

				@Override
				public T next() {
					return iterFirst.hasNext() ? iterFirst.next() : iterSecond.next();
				}
			};
		};
	}

	/**
	 * @param coll une collection
	 * @return le premier élément ou <b>null</b> si la collection est vide ou nulle;
	 */
	@Nullable
	public static <T> T getFirst(@Nullable Collection<T> coll) {
		if (coll == null || coll.isEmpty()) {
			return null;
		}
		return coll.iterator().next();
	}

	/**
	 * @param list une liste d'éléments
	 * @param <T>  le type d'élément dans la liste
	 * @return le dernier élément de la liste (utilise l'accès direct si la liste implémente {@link RandomAccess} ou un accès par {@link ListIterator} dans le cas contraire)
	 * @throws IllegalArgumentException si la liste est nulle ou vide
	 */
	public static <T> T getLastElement(List<T> list) {
		if (list == null || list.isEmpty()) {
			throw new IllegalArgumentException("Empty list has no last element!");
		}
		if (list instanceof RandomAccess) {
			return list.get(list.size() - 1);
		}
		else {
			return list.listIterator(list.size()).previous();
		}
	}

	/**
	 * @param col                   collection à transformer en chaîne de caractères
	 * @param renderer              le {@link ch.vd.unireg.common.StringRenderer} à utiliser pour les éléments de la collection
	 * @param separator             le séparateur à placer entre la représentation de chacun des éléments de la collection
	 * @param emptyCollectionString la chaîne de caractère à afficher dans le cas d'une collection nulle ou vide
	 * @param <T>                   type des éléments de la collection
	 * @return une chaîne de caractère qui énumère les éléments de la collection, séparés par le séparateur donné (les nulls de la collection sont ignorés)
	 */
	public static <T> String toString(Collection<T> col, StringRenderer<? super T> renderer, String separator, String emptyCollectionString) {
		if (col == null || col.isEmpty()) {
			return emptyCollectionString;
		}
		final StringBuilder b = new StringBuilder();
		for (T elt : col) {
			if (elt != null) {
				if (b.length() > 0) {
					b.append(separator);
				}
				b.append(renderer.toString(elt));
			}
		}
		return b.toString();
	}

	/**
	 * @param col       collection à transformer en chaîne de caractères
	 * @param renderer  le {@link ch.vd.unireg.common.StringRenderer} à utiliser pour les éléments de la collection
	 * @param separator le séparateur à placer entre la représentation de chacun des éléments de la collection
	 * @param <T>       type des éléments de la collection
	 * @return une chaîne de caractère qui énumère les éléments de la collection, séparés par le séparateur donné (en cas de collection nulle ou vide, renvoie une chaîne vide)
	 */
	public static <T> String toString(Collection<T> col, StringRenderer<? super T> renderer, String separator) {
		return toString(col, renderer, separator, StringUtils.EMPTY);
	}

	private static final StringRenderer<String> STRING_TRIMMER = StringUtils::trimToEmpty;

	public static String concat(List<String> list, String separator) {
		return toString(list, STRING_TRIMMER, separator);
	}

	@NotNull
	public static <K, V> Map<K, V> unmodifiableNeverNull(@Nullable Map<? extends K, ? extends V> source) {
		return source != null ? Collections.unmodifiableMap(source) : Collections.emptyMap();
	}

	@NotNull
	public static <T> List<T> unmodifiableNeverNull(@Nullable List<? extends T> source) {
		return source != null ? Collections.unmodifiableList(source) : Collections.emptyList();
	}

	@NotNull
	public static <T> Set<T> unmodifiableNeverNull(@Nullable Set<? extends T> source) {
		return source != null ? Collections.unmodifiableSet(source) : Collections.emptySet();
	}

	/**
	 * Analyse les deux collections fournies et supprime des collections les éléments communs.
	 * <p/>
	 * A noter que cette méthode tient compte des éléments dupliqués de manière unitaire. Ainsi,
	 * si un élément A est présent deux fois dans la première collection et seulement une fois
	 * dans la seconde collection alors il restera au final un élément A dans le première
	 * collection et zéro dans la seconde collection :
	 * <pre>
	 *     (a, a, b, c) + (a, d, e) => (a, b, c) + (d, e)
	 * </pre>
	 *
	 * @param left            une collection
	 * @param right           une autre collection
	 * @param equalityFunctor la function qui permet de détecter les éléments communs (optionnel)
	 * @param <T>             le type des éléments des collections
	 */
	public static <T> void removeCommonElements(@NotNull Collection<? extends T> left,
	                                            @NotNull Collection<? extends T> right,
	                                            @Nullable Equalator<T> equalityFunctor) {

		if (left == right) {
			// pas besoin de se casser la tête
			left.clear();
			return;
		}

		if (equalityFunctor == null) {
			equalityFunctor = T::equals;
		}

		final Iterator<? extends T> liter = left.iterator();
		while (liter.hasNext()) {
			final T l = liter.next();

			final Iterator<? extends T> riter = right.iterator();
			while (riter.hasNext()) {
				final T r = riter.next();

				if (equalityFunctor.test(l, r)) {
					// les deux éléments sont équivalents, on les supprime donc des deux listes.
					liter.remove();
					riter.remove();
					break;
				}
			}
		}
	}

	/**
	 * Analyse les deux collections fournies, supprime des collections les éléments communs pour les retourner.
	 * <p/>
	 * A noter que cette méthode tient compte des éléments dupliqués de manière unitaire. Ainsi,
	 * si un élément A est présent deux fois dans la première collection et seulement une fois
	 * dans la seconde collection alors il restera au final un élément A dans le première
	 * collection et zéro dans la seconde collection :
	 * <pre>
	 *     (a, a, b, c) + (a, d, e) => (a, b, c) + (d, e)
	 * </pre>
	 *
	 * @param left            une collection
	 * @param right           une autre collection
	 * @param equalityFunctor la function qui permet de détecter les éléments communs (optionnel)
	 * @param <T>             le type des éléments des collections
	 * @return la liste des paires d'éléments communs
	 */
	public static <T> List<Pair<T, T>> extractCommonElements(@NotNull Collection<? extends T> left,
	                                                         @NotNull Collection<? extends T> right,
	                                                         @Nullable Equalator<T> equalityFunctor) {

		if (equalityFunctor == null) {
			equalityFunctor = T::equals;
		}

		final List<Pair<T, T>> common = new LinkedList<>();

		final Iterator<? extends T> liter = left.iterator();
		while (liter.hasNext()) {
			final T l = liter.next();

			final Iterator<? extends T> riter = right.iterator();
			while (riter.hasNext()) {
				final T r = riter.next();

				if (equalityFunctor.test(l, r)) {
					// les deux éléments sont équivalents, on les supprime donc des deux listes et on les insère dans la liste commune
					liter.remove();
					riter.remove();
					common.add(Pair.of(l, r));
					break;
				}
			}
		}

		return common;
	}

	/**
	 * @param map           map à transformer en chaîne de caractères
	 * @param keyRenderer   le {@link ch.vd.unireg.common.StringRenderer} à utiliser pour les clé de la map
	 * @param valueRenderer le {@link ch.vd.unireg.common.StringRenderer} à utiliser pour les valeurs de la map
	 * @param separator     le séparateur à placer entre la représentation de chacun des éléments de la collection
	 * @param prefix        préfixe général de la chaîne de caractères
	 * @param suffix        suffixe général de la chaîne de caractères
	 * @param nullMapValue  valeur à renvoyer si la map est <code>null</code>
	 * @param <K>           type des clés de la map
	 * @param <V>           type des valeurs de la map
	 * @return une chaîne de caractère qui énumère les éléments de la collection, séparés par le séparateur donné
	 */
	public static <K, V> String toString(Map<K, V> map,
	                                     StringRenderer<? super K> keyRenderer,
	                                     StringRenderer<? super V> valueRenderer,
	                                     String separator,
	                                     String prefix,
	                                     String suffix,
	                                     String nullMapValue) {
		if (map == null) {
			return nullMapValue;
		}

		return map.entrySet().stream()
				.map(entry -> String.format("%s -> %s", keyRenderer.toString(entry.getKey()), valueRenderer.toString(entry.getValue())))
				.collect(Collectors.joining(separator, prefix, suffix));
	}

	/**
	 * @param coll une collection qui peut être nulle.
	 * @param <T>  le type d'éléments de la liste
	 * @return une liste non-nulle
	 */
	@NotNull
	public static <T> List<T> newList(@Nullable Collection<T> coll) {
		if (coll == null) {
			return Collections.emptyList();
		}
		else {
			return new ArrayList<>(coll);
		}
	}

	/**
	 * Transforme les éléments d'une collection en appliquant une fonction de mapping. La transformation est exécutée en parallèle sur plusieurs threads
	 * (threads fournis par l'ExecutorService spécifié). Cette méthode est <b>compatible</b> avec les transactions Spring (à l'inverse du ForkJoinPool qui peut
	 * faire du work-stealing et provoquer de graves problèmes avec les transactions Spring, voir SIFISC-27817).
	 *
	 * @param coll            une collection
	 * @param mapper          une fonction de mapping
	 * @param executorService un executorService
	 * @param <T>             le type des éléments entrant
	 * @param <R>             le type des éléments sortant
	 * @return une list avec les éléments mappés.
	 */
	public static <T, R> List<R> parallelMap(@NotNull Collection<T> coll, @NotNull Function<? super T, ? extends R> mapper, @NotNull ExecutorService executorService) {

		// on soumet les traitements asynchrones
		final List<CompletableFuture<R>> futures = new ArrayList<>(coll.size());
		coll.forEach(t -> {
			final CompletableFuture<R> future = CompletableFuture.supplyAsync(() -> mapper.apply(t), executorService);
			futures.add(future);
		});

		// on récupère le résultats des traitements asynchrones
		final List<R> results = new ArrayList<>(coll.size());
		futures.forEach(f -> results.add(getFuture(f)));

		return results;
	}

	private static <T> T getFuture(CompletableFuture<T> f) {
		try {
			return f.get();
		}
		catch (InterruptedException | ExecutionException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Construit un predicate qui permet d'appliquer un 'distinct' sur un stream en se basant sur une propriété des objets du stream (voir https://stackoverflow.com/a/27872852/593768)
	 * <p/>
	 * Exemple :
	 * <pre>
	 *     .filter(CollectionsUtils.distinctByKey(ModeleCommunauteRF::getId))
	 * </pre>
	 *
	 * @param keyExtractor l'extracteur de la propriété sur lequel appliquer le distinct
	 * @return un predicate à appliquer à une méthode 'filter'.
	 */
	public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
		Set<Object> seen = ConcurrentHashMap.newKeySet();
		return t -> seen.add(keyExtractor.apply(t));
	}
}
