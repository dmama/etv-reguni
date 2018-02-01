package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.collector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Stream;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Equalator;

/**
 * Spécificité de collecteur de données dont le résultat est exprimable sous la forme d'une liste à une seule dimension
 * et dont la donnée à historiser est présente plusieurs fois par snapshot
 * @param <S> type du snapshot
 * @param <D> type de la donnée à historiser
 */
public class MultiValueDataCollector<S, D, K> extends ListDataCollector<S, D> {

	/**
	 * Extracteur des données historisées depuis le snapshot
	 */
	private final Function<S, Stream<? extends D>> dataExtractor;

	/**
	 * Extracteur de la clé de regroupement depuis la donnée extraite (deux clés différentes seront historisées séparément)
	 */
	private final Function<? super D, K> groupingKeyExtractor;

	/**
	 * Données consolidées au fur et à mesure de l'analyse
	 */
	private final Map<K, NavigableMap<RegDate, DateRangeHelper.Ranged<D>>> groupings = new HashMap<>();

	/**
	 * @param dataExtractor extracteur des données historisées depuis le snapshot
	 * @param dataEqualator prédicat qui permet de dire si une donnée extraite est restée idendique ou pas
	 * @param groupingKeyExtractor extracteur de la clé de regroupement depuis la donnée extraite (deux clés différentes seront historisées séparément)
	 *                             NOTE: L'objet retourné pour servir de clé doit impérativement implémenter equals() et hashcode()
	 */
	public MultiValueDataCollector(Function<S, Stream<? extends D>> dataExtractor, Equalator<? super D> dataEqualator, Function<? super D, K> groupingKeyExtractor) {
		super(dataEqualator);
		this.dataExtractor = dataExtractor;
		this.groupingKeyExtractor = groupingKeyExtractor;
	}

	@Override
	protected Stream<DateRangeHelper.Ranged<D>> getCollectedDataStream() {
		return groupings.values().stream()
				.map(DataCollector::getCollected)
				.flatMap(Function.identity());
	}

	/**
	 * Traitement d'une valeur qui sort sous la forme d'une collection
	 * @param date date du snapshot
	 * @param source données du snapshot
	 */
	@Override
	public void collect(RegDate date, S source) {
		final Stream<? extends D> stream = source == null ? Stream.empty() : dataExtractor.apply(source);

		// on fait d'abord le boulot pour les données présentes en collectant les clés passées en revue
		final Set<K> usedKeys = new HashSet<>();
		stream.forEach(d -> {
			final K key = groupingKeyExtractor.apply(d);
			usedKeys.add(key);

			final NavigableMap<RegDate, DateRangeHelper.Ranged<D>> keySpecificMap = getOrCreateSpecificMap(key, groupings);
			collect(keySpecificMap, dataEqualator, date, d);
		});

		// pour toutes les clés qui ne sont pas modifiées, il faut poser un "null" car les entités ont peut-être disparu juste là
		groupings.entrySet().stream()
				.filter(e -> !usedKeys.contains(e.getKey()))
				.map(Map.Entry::getValue)
				.forEach(col -> collect(col, dataEqualator, date, null));
	}

	/**
	 * @param key clé de regroupement
	 * @param groupings ensemble des regroupements
	 * @param <D> type de la donnée à historiser
	 * @param <K> type de la clé de regroupement
	 * @return la map (existante ou nouvellement créée) associée à la clé dans l'ensemble des regroupements
	 */
	private static <D, K> NavigableMap<RegDate, DateRangeHelper.Ranged<D>> getOrCreateSpecificMap(K key, Map<K, NavigableMap<RegDate, DateRangeHelper.Ranged<D>>> groupings) {
		final NavigableMap<RegDate, DateRangeHelper.Ranged<D>> existing = groupings.get(key);
		if (existing != null) {
			return existing;
		}

		final NavigableMap<RegDate, DateRangeHelper.Ranged<D>> newMap = new TreeMap<>();
		groupings.put(key, newMap);
		return newMap;
	}
}
