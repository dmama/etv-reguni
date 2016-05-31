package ch.vd.uniregctb.migration.pm.utils;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;

/**
 * Quelques méthodes utilitaires autour des données des organisations fournies par RCEnt
 */
public abstract class OrganisationDataHelper {

	/**
	 * @param ranges liste de valeurs avec leur validité temporelle (supposée triée chronologiquement)
	 * @param <T> type de la valeur attendue
	 * @return la dernière valeur du range (= la dernière valeur connue)
	 */
	@Nullable
	public static <T> T getLastValue(List<DateRanged<T>> ranges) {
		if (ranges == null || ranges.isEmpty()) {
			return null;
		}

		final DateRanged<T> range = ranges.get(ranges.size() - 1);
		return range != null ? range.getPayload() : null;
	}

	/**
	 * @param ranges liste de valeurs avec leur validité temporelle (supposée triée chronologiquement)
	 * @return la date de début de la première valeur (ou <code>null</code> en absence de première valeur...)
	 */
	@Nullable
	public static <T extends DateRange> RegDate getFirstKnownDate(List<T> ranges) {
		if (ranges == null || ranges.isEmpty()) {
			return null;
		}
		return ranges.get(0).getDateDebut();
	}

}
