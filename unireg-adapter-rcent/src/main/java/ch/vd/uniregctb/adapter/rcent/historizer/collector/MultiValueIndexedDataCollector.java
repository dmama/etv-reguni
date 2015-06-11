package ch.vd.uniregctb.adapter.rcent.historizer.collector;

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
import ch.vd.uniregctb.adapter.rcent.historizer.container.DateRanged;
import ch.vd.uniregctb.adapter.rcent.historizer.container.Keyed;
import ch.vd.uniregctb.adapter.rcent.historizer.equalator.Equalator;

/**
 * Spécificité de collecteur de données dont le résultat est exprimable sous la forme d'un ensemble de listes indexées par une clé de regroupement
 * quand la donnée est présente plusieurs fois par clé de regroupement
 * @param <S> type du snapshot
 * @param <D> type de la donnée à historiser
 * @param <KS> type de la clé de regroupement à la sortie
 * @param <KI> type de la clé de regroupement interne
 */
public class MultiValueIndexedDataCollector<S, D, KS, KI> extends IndexedDataCollector<S, D, KS> {

	private final Function<S, Stream<Keyed<KS, D>>> dataExtractor;
	private final Equalator<? super D> dataEqualator;
	private final Function<Keyed<KS, D>, KI> groupingKeyExtractor;
	private final Map<KS, MultiValueDataCollector<S, Keyed<KS, D>, KI>> groupings = new HashMap<>();

	/**
	 * @param dataExtractor extracteur des données du snapshot
	 * @param dataEqualator prédicat qui permet de dire si une donnée extraite est restée idendique ou pas
	 * @param groupingKeyExtractor extracteur de la clé (interne) de regroupement (ce seront pas les clés dans la map de résultats finaux)
	 *                             NOTE: L'objet retourné pour servir de clé doit impérativement implémenter equals() et hashcode()
	 */
	public MultiValueIndexedDataCollector(Function<S, Stream<Keyed<KS, D>>> dataExtractor,
	                                      Equalator<? super D> dataEqualator,
	                                      Function<Keyed<KS, D>, KI> groupingKeyExtractor) {

		this.dataExtractor = dataExtractor;
		this.dataEqualator = dataEqualator;
		this.groupingKeyExtractor = groupingKeyExtractor;
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
		final Function<Map.Entry<KS, MultiValueDataCollector<S, Keyed<KS, D>, KI>>, List<DateRanged<D>>> mapper =
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

		// on consigne toutes les clés vues (pour identifier plus loin celles que l'on n'a pas vues) et on collecte leurs données
		final Set<KS> usedKeys = new HashSet<>();
		stream.map(Keyed::getKey)
				.peek(usedKeys::add)
				.map(this::getOrCreateSpecificCollector)
				.forEach(specificCollector -> specificCollector.collect(date, snapshot));

		// pour toutes les clés pour lesquelles il n'y a rien eu, c'est que quelque chose a disparu
		groupings.entrySet().stream()
				.filter(entry -> !usedKeys.contains(entry.getKey()))
				.map(Map.Entry::getValue)
				.forEach(collector -> collector.collect(date, null));
	}

	@NotNull
	private MultiValueDataCollector<S, Keyed<KS, D>, KI> getOrCreateSpecificCollector(KS key) {
		final MultiValueDataCollector<S, Keyed<KS, D>, KI> existing = groupings.get(key);
		if (existing != null) {
			return existing;
		}

		final MultiValueDataCollector<S, Keyed<KS, D>, KI> newCollector = new MultiValueDataCollector<>(buildLocalDataExtractor(key, dataExtractor),
		                                                                                                buildKeyedDataEqualator(dataEqualator),
		                                                                                                groupingKeyExtractor);
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
