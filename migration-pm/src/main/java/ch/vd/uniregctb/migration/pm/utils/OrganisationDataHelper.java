package ch.vd.uniregctb.migration.pm.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.organisation.data.DateRanged;
import ch.vd.unireg.interfaces.organisation.data.OrganisationConstants;

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

	/**
	 * Renvoie la liste des numéros IDE historisés
	 * @param identifiers les identifiants connus d'une <i>organisation</i> (ou d'une <i>location</i>)
	 * @return la liste des numéros IDE
	 */
	@NotNull
	public static List<DateRanged<String>> getNumerosIDE(Map<String, List<DateRanged<String>>> identifiers) {
		return Optional.ofNullable(identifiers.get(OrganisationConstants.CLE_IDE)).orElseGet(Collections::emptyList);
	}
}
