package ch.vd.uniregctb.migration.pm.historizer.collector;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;
import ch.vd.uniregctb.migration.pm.historizer.equalator.Equalator;

/**
 * Spécificité de collecteur de données dont le résultat est exprimable sous la forme d'une liste à une seule dimension
 * @param <S> type du snapshot
 * @param <D> type de la donnée à historiser
 */
public abstract class LinearDataCollector<S, D> extends DataCollector<S> {

	/**
	 * Prédicat qui permet de dire si une donnée extraite est restée idendique ou pas
	 */
	protected final Equalator<? super D> dataEqualator;

	/**
	 * @param dataEqualator Predicat qui permet de dire si deux structures de données non-nulles sont identiques
	 */
	protected LinearDataCollector(Equalator<? super D> dataEqualator) {
		this.dataEqualator = dataEqualator;
	}

	/**
	 * @return la liste des données historisées disponible après analyse
	 */
	public final List<DateRanged<D>> getCollectedData() {
		return getCollectedData(ArrayList::new);
	}

	/**
	 * @param listFactory constructeur de liste spécifique
	 * @return la liste des données historisées disponible après analyse
	 */
	public final List<DateRanged<D>> getCollectedData(Supplier<List<DateRanged<D>>> listFactory) {
		return getCollectedDataStream().collect(Collectors.toCollection(listFactory));
	}

	/**
	 * @return le stream des données historisées, disponible après analyse
	 */
	protected abstract Stream<DateRanged<D>> getCollectedDataStream();
}
