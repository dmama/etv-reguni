package ch.vd.uniregctb.migration.pm.historizer.collector;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.historizer.container.Keyed;
import ch.vd.uniregctb.migration.pm.historizer.container.SequentialDateRangesChecker;
import ch.vd.uniregctb.migration.pm.historizer.equalator.Equalator;

/**
 * Spécificité de collecteur de données dont le résultat est exprimable sous la forme d'un ensemble de listes indexées par une clé de regroupement
 * quand la donnée est présente une seule fois par clé de regroupement
 * @param <S> type du snapshot
 * @param <D> type de la donnée à historiser
 * @param <K> type de la clé de regroupement
 */
public class SingleValueIndexedDataCollector<S, D, K> extends IndexedDataCollector<S, D, K> {

	/**
	 * Clé unique interne afin que toutes les données extraites soient considérées ensemble (= single value)
	 */
	private static final Object SINGLE_KEY = new Object();

	/**
	 * On délègue à un collecteur multi-valeurs pour éviter de dupliquer la logique assez complexe qui
	 * entoure la gestion des index.
	 */
	private final IndexedDataCollector<S, D, K> delegateCollector;

	/**
	 * @param dataExtractor extracteur de la donnée du snapshot
	 * @param dataEqualator prédicat qui permet de dire si une donnée extraite est restée idendique ou pas
	 */
	public SingleValueIndexedDataCollector(Function<S, Stream<Keyed<K, D>>> dataExtractor,
	                                       Equalator<? super D> dataEqualator) {
		this.delegateCollector = new MultiValueIndexedDataCollector<>(dataExtractor,
		                                                            dataEqualator,
		                                                            d -> SINGLE_KEY);
		                                                            // On n'a qu'une seule valeur,
		                                                            // la clé de groupement doit être
		                                                            // une valeur unique immuable.
	}

	/**
	 * @param mapFactory constructeur de la map renvoyée
	 * @param listFactory constructeur des listes utilisées comme valeurs dans la map renvoyée
	 * @return les données historisées disponibles après analyse
	 */
	@Override
	public final Map<K, List<DateRanged<D>>> getCollectedData(Supplier<Map<K, List<DateRanged<D>>>> mapFactory, Supplier<List<DateRanged<D>>> listFactory) {
		Map<K, List<DateRanged<D>>> collectedData = delegateCollector.getCollectedData(mapFactory, listFactory);
		collectedData.values().forEach(SequentialDateRangesChecker::ensureSequential);
		return collectedData;
	}

	@Override
	public void collect(RegDate date, S snapshot) {
		delegateCollector.collect(date, snapshot);
	}
}
