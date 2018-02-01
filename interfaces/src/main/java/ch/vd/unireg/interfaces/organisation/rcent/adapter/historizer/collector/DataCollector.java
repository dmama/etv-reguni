package ch.vd.unireg.interfaces.organisation.rcent.adapter.historizer.collector;

import java.util.NavigableMap;
import java.util.stream.Stream;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.Equalator;

/**
 * Classe de base des collecteurs de données
 * @param <S> le type de la structure de données des snapshots/photos
 */
public abstract class DataCollector<S> {

	/**
	 * Méthode appelée par le moteur de calcul pour chacun des snapshots
	 * @param date date du snapshot
	 * @param snapshot données du snapshot
	 */
	public abstract void collect(RegDate date, S snapshot);

	/**
	 * Méthode de calcul de données avec plage de validité
	 * @param collected les données collectées jusque là
	 * @param dataEqualator prédicat qui permet de dire si deux données non nulles sont égales
	 * @param date la date de validité d'une nouvelle valeur
	 * @param newValue la nouvelle valeur en question
	 */
	protected static <T> void collect(NavigableMap<RegDate, DateRangeHelper.Ranged<T>> collected, Equalator<? super T> dataEqualator, RegDate date, @Nullable T newValue) {
		final DateRangeHelper.Ranged<T> lastRangedValue = collected.isEmpty() ? null : collected.lastEntry().getValue();
		final T lastValue = lastRangedValue == null ? null : lastRangedValue.getPayload();
		if (lastValue == null) {
			if (newValue != null) {
				collected.put(date, new DateRangeHelper.Ranged<>(date, null, newValue));
			}
		}
		else if (newValue == null || !dataEqualator.test(lastValue, newValue)) {
			if (lastRangedValue.getDateDebut().isAfterOrEqual(date)) {
				panic();
			}
			collected.put(lastRangedValue.getDateDebut(), lastRangedValue.withDateFin(date.getOneDayBefore()));
			collected.put(date, new DateRangeHelper.Ranged<>(date, null, newValue));
		}
	}

	private static void panic() {
		throw new IllegalArgumentException("A date identical to or greater than the starting date of the previous period has been encountered.\n" +
				                                   "  This can mean a few things, such as:\n" +
				                                   "  - There more than one snapshot per day. Only one is allowed in the incoming snapshot stream.\n" +
				                                   "  - Multiple values are collected per snapshot for a single field/entity, where only one is expected. Check the DataCollector in use.\n" +
				                                   "  - Snapshots are processed out of orders. Cannot happen. [Historizer bug]\n");
	}

	/**
	 * @param collected la map des données collectées
	 * @param <T> le type des données auxquelles on rajoute une plage de validité
	 * @return un stream qui fournit les données à exposer depuis la map
	 */
	protected static <T> Stream<DateRangeHelper.Ranged<T>> getCollected(NavigableMap<?, DateRangeHelper.Ranged<T>> collected) {
		return collected.values().stream().filter(v -> v.getPayload() != null);
	}
}
