package ch.vd.unireg.interfaces.organisation.mock.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.registre.base.utils.NotImplementedException;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;

public abstract class MockOrganisationHelper {

	/**
	 * @param map une map de snapshots à laquelle il faut rajouter une donnée
	 * @param date la date de référence de la nouvelle donnée
	 * @param newValue la nouvelle donnée (l'absence de donnée est indiquée par une valeur <code>null</code>) qui ne sortira pas dans l'historique généré par {@link #getHisto(SortedMap)}
	 * @param <T> le type de la donnée
	 */
	public static <T> void changeRangedData(SortedMap<RegDate, T> map, RegDate date, @Nullable T newValue) {
		if (map.put(date, newValue) != null) {
			throw new NotImplementedException("Le cas de plusieurs données à la même date n'est pas supporté dans ce Mock...");
		}
	}

	/**
	 * Explose si un autre changement a déjà été demandé entre les dates de début et de fin...
	 */
	public static <T> void addRangedData(NavigableMap<RegDate, T> map, @NotNull RegDate dateDebut, RegDate dateFin, @Nullable T newValue) {
		final Map.Entry<RegDate, T> previous = map.floorEntry(dateDebut);
		final Map.Entry<RegDate, T> next = map.higherEntry(dateDebut);
		if (next != null && RegDateHelper.isAfterOrEqual(dateFin, next.getKey(), NullDateBehavior.LATEST)) {
			throw new IllegalArgumentException("Conflit de range : une valeur a déjà été entrée pour la date " + next.getKey());
		}
		if (previous != null && dateDebut == previous.getKey()) {
			throw new IllegalArgumentException("Conflit de range : une valeur a déjà été entrée pour la date " + dateDebut);
		}
		map.put(dateDebut, newValue);
		if (previous != null && dateFin != null) {
			map.put(dateFin.getOneDayAfter(), previous.getValue());
		}
	}

	/**
	 * Reconstruction d'une liste de valeur historisées à partir d'une map (= quelque part, ce sont des spnapshots)
	 * @param map la map des valeurs (= snapshots)
	 * @param <T> le type de la valeur présente dans les snapshots
	 * @return la liste des valeurs historisées
	 */
	@NotNull
	public static <T> List<DateRanged<T>> getHisto(SortedMap<RegDate, T> map) {
		final NavigableMap<RegDate, DateRangeHelper.Ranged<T>> rangedMap = new TreeMap<>();
		for (Map.Entry<RegDate, T> entry : map.entrySet()) {
			final RegDate ref = entry.getKey();
			final Map.Entry<RegDate, DateRangeHelper.Ranged<T>> previous = rangedMap.lowerEntry(ref);
			if (previous != null) {
				rangedMap.put(previous.getKey(), previous.getValue().withDateFin(ref.getOneDayBefore()));
			}
			rangedMap.put(ref, new DateRangeHelper.Ranged<>(ref, null, entry.getValue()));
		}

		// on filtre les valeurs nulles
		final List<DateRanged<T>> histo = new ArrayList<>(rangedMap.size());
		for (DateRangeHelper.Ranged<T> ranged : rangedMap.values()) {
			final T payload = ranged.getPayload();
			if (payload != null) {
				histo.add(new DateRanged<>(ranged.getDateDebut(), ranged.getDateFin(), payload));
			}
		}
		return histo;
	}

}
