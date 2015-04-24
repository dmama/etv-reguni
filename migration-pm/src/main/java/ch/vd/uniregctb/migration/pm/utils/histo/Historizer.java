package ch.vd.uniregctb.migration.pm.utils.histo;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;

/**
 * Utility class which is able to extract historized data (= with time validity dates)
 * from snapshots indexed by begin validity dates
 */
public abstract class Historizer {

	/**
	 * Entrée principale de la moulinette d'historisation (= de constitution de données associées à des plages de dates)
	 * @param source map des photos indexées par date
	 * @param collectors collecteurs pour la moulinette de constitution des données associées à des plages de dates
	 * @param <S> type de la données des photos
	 */
	public static <S> void historize(Map<RegDate, S> source, List<? extends DataCollector<? super S>> collectors) {
		if (source == null || collectors == null || source.isEmpty() || collectors.isEmpty()) {
			return;
		}

		// il est important de passer les snapshots dans l'ordre croissant...
		source.entrySet().stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))
				.forEach(entry -> collectors.stream().forEach(dataCollector -> dataCollector.collect(entry.getKey(), entry.getValue())));
	}

}
