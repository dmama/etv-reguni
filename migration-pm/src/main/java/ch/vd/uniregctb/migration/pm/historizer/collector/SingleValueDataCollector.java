package ch.vd.uniregctb.migration.pm.historizer.collector;

import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.historizer.container.SequentialDateRangesChecker;
import ch.vd.uniregctb.migration.pm.historizer.equalator.Equalator;

/**
 * Spécificité de collecteur de données dont le résultat est exprimable sous la forme d'une liste à une seule dimension
 * et dont la donnée à historisée n'est présente qu'une seule fois par snapshot
 * @param <S> type du snapshot
 * @param <D> type de la donnée à historiser
 */
public class SingleValueDataCollector<S, D> extends ListDataCollector<S, D> {

	/**
	 * Extracteur de la donnée du snapshot
	 */
	private final Function<S, ? extends D> dataExtractor;

	/**
	 * Données consolidées au fur et à mesure de l'analyse
	 */
	private final NavigableMap<RegDate, DateRanged<D>> collected = new TreeMap<>();

	/**
	 * @param dataExtractor extracteur de la donnée du snapshot
	 * @param dataEqualator prédicat qui permet de dire si une donnée extraite est restée idendique ou pas
	 */
	public SingleValueDataCollector(Function<S, ? extends D> dataExtractor, Equalator<? super D> dataEqualator) {
		super(dataEqualator);
		this.dataExtractor = dataExtractor;
	}

	@Override
	protected Stream<DateRanged<D>> getCollectedDataStream() {
		return getCollected(collected);
	}

	@Override
	public final List<DateRanged<D>> getCollectedData(Supplier<List<DateRanged<D>>> listFactory) {
		List<DateRanged<D>> collectedData = getCollectedDataStream().collect(Collectors.toCollection(listFactory));
		SequentialDateRangesChecker.ensureSequential(collectedData);
		return collectedData;
	}


	@Override
	public void collect(RegDate date, S snapshot) {
		final D currentValue = dataExtractor.apply(snapshot);
		collect(collected, dataEqualator, date, currentValue);
	}
}
