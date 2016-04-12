package ch.vd.uniregctb.migration.pm.utils;

import java.util.Collections;
import java.util.Objects;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;

/**
 * Quelques méthodes pratiques pour les calculs autour d'un graphe
 */
public class GrapheHelper {

	private final boolean rcentEnabled;

	public GrapheHelper(boolean rcentEnabled) {
		this.rcentEnabled = rcentEnabled;
	}

	/**
	 * @param graphe un graphe d'entités à migrer
	 * @return une collection triée des identifiants cantonaux connus dans ce graphe (au travers des entreprises et des établissements du graphe, vide si RCEnt n'est pas activé)
	 */
	public SortedSet<Long> extractNumerosCantonaux(Graphe graphe) {
		if (rcentEnabled) {
			final Stream<Long> fromEntreprises = graphe.getEntreprises().values().stream().map(RegpmEntreprise::getNumeroCantonal);
			final Stream<Long> fromEtablissements = graphe.getEtablissements().values().stream().map(RegpmEtablissement::getNumeroCantonal);
			return Stream.concat(fromEntreprises, fromEtablissements)
					.filter(Objects::nonNull)
					.collect(Collectors.toCollection(TreeSet::new));
		}
		else {
			return Collections.emptyNavigableSet();
		}
	}
}
