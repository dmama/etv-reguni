package ch.vd.uniregctb.migration.pm.historizer.collector;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;
import ch.vd.uniregctb.migration.pm.historizer.equalator.Equalator;

/**
 * Spécificité de collecteur de données dont le résultat est exprimable sous la forme d'un ensemble de listes indexées par une clé de regroupement
 * quand la donnée est présente plusieurs fois par clé de regroupement
 * @param <S> type du snapshot
 * @param <D> type de la donnée à historiser
 * @param <KS> type de la clé de regroupement à la sortie
 * @param <KI> type de la clé de regroupement interne
 */
public class FlattenIndexedDataCollector<S, D, KS, KI> extends IndexedDataCollector<S, D, KS> {

	private final Function<S, Stream<Keyed<KS, D>>> dataExtractor;
	private final Equalator<? super D> dataEqualator;
	private final Function<Keyed<KS, D>, KI> keyExtractor;
	private final Map<KS, FlattenDataCollector<S, Keyed<KS, D>, KI>> groupings = new HashMap<>();

	/**
	 * @param dataExtractor extracteur des données du snapshot
	 * @param dataEqualator prédicat qui permet de dire si une donnée extraite est restée idendique ou pas
	 * @param keyExtractor extracteur de la clé (externe) de regroupement (ce seront les clés dans la map de résultats finaux)
	 */
	public FlattenIndexedDataCollector(Function<S, Stream<Keyed<KS, D>>> dataExtractor,
	                                   Equalator<? super D> dataEqualator,
	                                   Function<Keyed<KS, D>, KI> keyExtractor) {

		this.dataExtractor = dataExtractor;
		this.dataEqualator = dataEqualator;
		this.keyExtractor = keyExtractor;
	}

	/**
	 * Construit un extracteur de clé "depuis une construction" à partir d'un extracteur de clé "simple"
	 * @param simple l'extracteur de clé simple (qui suppose que la clé est extractable des données de l'objet seul)
	 * @param <D> type de la donnée
	 * @param <KS> type de la clé (externe) de regoupement finalement pas nécessaire pour la clé de regroupement interne
	 * @param <KI> type de la clé de regroupement interne
	 * @return l'extracteur demandé utilisable dans le constructeur de cette classe
	 */
	@NotNull
	public static <D, KS, KI> Function<Keyed<KS, D>, KI> enkey(Function<? super D, KI> simple) {
		return keyed -> simple.apply(keyed.getValue());
	}

	@Override
	public Map<KS, List<DateRanged<D>>> getCollectedData(Supplier<Map<KS, List<DateRanged<D>>>> mapFactory, Supplier<List<DateRanged<D>>> listFactory) {
		final Function<Map.Entry<KS, FlattenDataCollector<S, Keyed<KS, D>, KI>>, List<DateRanged<D>>> mapper =
				entry -> entry.getValue().getCollectedDataStream()
						.map(d -> new DateRanged<>(d.getDateDebut(), d.getDateFin(), d.getPayload().getValue()))
						.collect(Collectors.toCollection(listFactory));

		final BinaryOperator<List<DateRanged<D>>> merger = (l1, l2) -> Stream.concat(l1.stream(), l2.stream()).collect(Collectors.toCollection(listFactory));

		return groupings.entrySet().stream()
				.collect(Collectors.toMap(Map.Entry::getKey, mapper, merger, mapFactory));
	}

	@Override
	public void collect(RegDate date, S snapshot) {
		final Stream<Keyed<KS, D>> stream = snapshot == null ? Stream.empty() : dataExtractor.apply(snapshot);

		final Set<KS> usedKeys = new HashSet<>();
		stream.forEach(keyed -> {
			final KS key = keyed.getKey();
			usedKeys.add(key);

			final FlattenDataCollector<S, Keyed<KS, D>, KI> keySpecificCollector = getOrCreateSpecificCollector(key);
			keySpecificCollector.collect(date, snapshot);
		});

		// pour toutes les clés pour lesquelles il n'y a rien eu, c'est que quelque chose a disparu
		groupings.entrySet().stream()
				.filter(entry -> !usedKeys.contains(entry.getKey()))
				.map(Map.Entry::getValue)
				.forEach(collector -> collector.collect(date, null));
	}

	@NotNull
	private FlattenDataCollector<S, Keyed<KS, D>, KI> getOrCreateSpecificCollector(KS key) {
		final FlattenDataCollector<S, Keyed<KS, D>, KI> existing = groupings.get(key);
		if (existing != null) {
			return existing;
		}

		final FlattenDataCollector<S, Keyed<KS, D>, KI> newCollector = new FlattenDataCollector<>(buildLocalDataExtractor(key, dataExtractor),
		                                                                                          buildKeyedDataEqualator(dataEqualator),
		                                                                                          keyExtractor);
		groupings.put(key, newCollector);
		return newCollector;
	}

	@NotNull
	private static <KS, D> Equalator<Keyed<KS, D>> buildKeyedDataEqualator(Equalator<? super D> dataEqualator) {
		return (k1, k2) -> Equalator.DEFAULT.test(k1.getKey(), k2.getKey()) && dataEqualator.test(k1.getValue(), k2.getValue());
	}

	@NotNull
	private static <S, D, KS> Function<S, Stream<? extends Keyed<KS, D>>> buildLocalDataExtractor(KS key, Function<S, Stream<Keyed<KS, D>>> source) {
		return s -> source.apply(s)
				.filter(keyed -> Equalator.DEFAULT.test(keyed.getKey(), key));
	}
}
