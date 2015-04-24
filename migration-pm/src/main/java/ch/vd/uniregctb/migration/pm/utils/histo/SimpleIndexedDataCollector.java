package ch.vd.uniregctb.migration.pm.utils.histo;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.utils.Equalator;

/**
 * Spécificité de collecteur de données dont le résultat est exprimable sous la forme d'un ensemble de listes indexées par une clé de regroupement
 * quand la donnée est présente une seule fois par clé de regroupement
 * @param <S> type du snapshot
 * @param <D> type de la donnée à historiser
 * @param <K> type de la clé de regroupement
 */
public class SimpleIndexedDataCollector<S, D, K> extends IndexedDataCollector<S, D, K> {

	private final LinearDataCollector<S, Keyed<K, D>> targetCollector;

	/**
	 * @param dataExtractor extracteur de la donnée du snapshot
	 * @param dataEqualator prédicat qui permet de dire si une donnée extraite est restée idendique ou pas
	 */
	public SimpleIndexedDataCollector(Function<S, ? extends D> dataExtractor,
	                                  Equalator<? super D> dataEqualator,
	                                  Function<? super D, ? extends K> keyExtractor) {

		this.targetCollector = new SimpleDataCollector<>(buildKeyedDataExtractor(dataExtractor, keyExtractor),
		                                                 buildKeyedDataEqualator(dataEqualator));
	}

	@NotNull
	private static <S, D, K> Function<S, ? extends Keyed<K, D>> buildKeyedDataExtractor(Function<S, ? extends D> dataExtractor,
	                                                                                    Function<? super D, ? extends K> keyExtractor) {
		return s -> {
			final D data = dataExtractor.apply(s);
			final K key = keyExtractor.apply(data);
			return new Keyed<>(key, data);
		};
	}

	@NotNull
	protected static <D> Equalator<Keyed<?, D>> buildKeyedDataEqualator(Equalator<? super D> dataEqualator) {
		return (o1, o2) -> Equalator.DEFAULT.test(o1.getKey(), o2.getKey()) && dataEqualator.test(o1.getValue(), o2.getValue());
	}

	/**
	 * @param mapFactory constructeur de la map renvoyée
	 * @param listFactory constructeur des listes utilisées comme valeurs dans la map renvoyée
	 * @return les données historisées disponibles après analyse
	 */
	@Override
	public final Map<K, List<DateRanged<D>>> getCollectedData(Supplier<Map<K, List<DateRanged<D>>>> mapFactory, Supplier<List<DateRanged<D>>> listFactory) {
		final Stream<DateRanged<Keyed<K, D>>> keyedStream = targetCollector.getCollectedDataStream();;
		final Function<DateRanged<Keyed<K, D>>, K> subKeyExtractor = d -> d.getPayload().getKey();
		final Function<DateRanged<Keyed<K, D>>, DateRanged<D>> subDataExtractor = d -> new DateRanged<>(d.getDateDebut(), d.getDateFin(), d.getPayload().getValue());
		return keyedStream.collect(Collectors.groupingBy(subKeyExtractor, mapFactory, Collectors.mapping(subDataExtractor, Collectors.toCollection(listFactory))));
	}

	@Override
	protected void collect(RegDate date, S snapshot) {
		targetCollector.collect(date, snapshot);
	}
}
