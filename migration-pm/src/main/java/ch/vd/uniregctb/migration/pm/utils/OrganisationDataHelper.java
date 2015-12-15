package ch.vd.uniregctb.migration.pm.utils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeHelper;
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
	public static <T> T getLastValue(List<DateRangeHelper.Ranged<T>> ranges) {
		if (ranges == null || ranges.isEmpty()) {
			return null;
		}

		final DateRangeHelper.Ranged<T> range = ranges.get(ranges.size() - 1);
		return range != null ? range.getPayload() : null;
	}

	/**
	 * Renvoie la liste des numéros IDE historisés
	 * @param identifiers les identifiants connus d'une <i>organisation</i> (ou d'une <i>location</i>)
	 * @return la liste des numéros IDE
	 */
	@NotNull
	public static List<DateRangeHelper.Ranged<String>> getNumerosIDE(Map<String, List<DateRangeHelper.Ranged<String>>> identifiers) {
		return Optional.ofNullable(identifiers.get(OrganisationConstants.CLE_IDE)).orElseGet(Collections::emptyList);
	}
}
