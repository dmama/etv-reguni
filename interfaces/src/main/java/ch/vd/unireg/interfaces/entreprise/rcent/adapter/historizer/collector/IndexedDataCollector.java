package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.collector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import ch.vd.registre.base.date.DateRangeHelper;

/**
 * Spécificité de collecteur de données dont le résultat est exprimable sous la forme d'un ensemble de listes indexées par une clé de regroupement
 * @param <S> type du snapshot
 * @param <D> type de la donnée à historiser
 * @param <K> type de la clé de regroupement
 */
public abstract class IndexedDataCollector<S, D, K> extends DataCollector<S> {

	/**
	 * @return les données historisées disponibles après analyse
	 */
	public final Map<K, List<DateRangeHelper.Ranged<D>>> getCollectedData() {
		return getCollectedData(HashMap::new, ArrayList::new);
	}

	/**
	 * @param mapFactory constructeur de la map renvoyée
	 * @param listFactory constructeur des listes utilisées comme valeurs dans la map renvoyée
	 * @return les données historisées disponibles après analyse
	 */
	public abstract Map<K, List<DateRangeHelper.Ranged<D>>> getCollectedData(Supplier<Map<K, List<DateRangeHelper.Ranged<D>>>> mapFactory, Supplier<List<DateRangeHelper.Ranged<D>>> listFactory);
}
