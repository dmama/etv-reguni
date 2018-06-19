package ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.historizer.collector.DataCollector;

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
			throw new IllegalArgumentException("Source or collectors is empty or null. At least one snapshot " +
					                                   "in the source and one collector is required.");
		}

		/*
			Double boucle de génération d'historique.
		 */

		source.entrySet().stream()
				.sorted(Comparator.comparing(Map.Entry::getKey))  // -----------------------------------------------------> Il est crucial d'assurer l'ordre chronologique (voir ci-dessous)
				.forEach( //----------------------------------------------------------------------------------------------> Pour chaque Snapshot RegDate <-> Graphe d'objets
				          entry -> collectors.stream()
						          .forEach( // ---------------------------------------------------------------------------> Pour chaque Collector donné en entrée
						                    dataCollector -> dataCollector.collect(entry.getKey(), entry.getValue()) // --> Le Collector est applique au snapshot.
						                    /*
						                     * Chaque Collector traite d'un type de donnée et génère une Collection de plages de temps (Time Periods) représentant l'évolution de cette donnée.
				                             * Chaque Collector conserve cette liste.
				                             * A chaque tour, il se sert de la dernière entrée, la comparant avec celle en cours de revue pour déterminer le résultat.
				                             */
				          )
				);
	}

}
