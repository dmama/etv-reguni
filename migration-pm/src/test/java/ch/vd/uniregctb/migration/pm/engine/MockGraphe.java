package ch.vd.uniregctb.migration.pm.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.migration.pm.Graphe;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEntreprise;
import ch.vd.uniregctb.migration.pm.regpm.RegpmEtablissement;
import ch.vd.uniregctb.migration.pm.regpm.RegpmIndividu;
import ch.vd.uniregctb.migration.pm.regpm.WithLongId;

/**
 * Implémentation de test de l'interface Graphe
 */
public class MockGraphe implements Graphe {

	private final Map<Long, RegpmEntreprise> entreprises;
	private final Map<Long, RegpmEtablissement> etablissements;
	private final Map<Long, RegpmIndividu> individus;

	public MockGraphe(Collection<RegpmEntreprise> entreprises,
	                  Collection<RegpmEtablissement> etablissements,
	                  Collection<RegpmIndividu> individus) {

		this.entreprises = buildMap(entreprises);
		this.etablissements = buildMap(etablissements);
		this.individus = buildMap(individus);
	}

	@Override
	public String toString() {
		final List<String> array = new ArrayList<>(3);
		if (!entreprises.isEmpty()) {
			array.add(String.format("%d entreprise(s) (%s)", entreprises.size(), Arrays.toString(entreprises.keySet().toArray(new Long[entreprises.size()]))));
		}
		if (!etablissements.isEmpty()) {
			array.add(String.format("%d établissement(s) (%s)", etablissements.size(), Arrays.toString(etablissements.keySet().toArray(new Long[etablissements.size()]))));
		}
		if (!individus.isEmpty()) {
			array.add(String.format("%d individu(s) (%s)", individus.size(), Arrays.toString(individus.keySet().toArray(new Long[individus.size()]))));
		}

		if (array.isEmpty()) {
			return "rien (???)";
		}
		else {
			return array.stream().collect(Collectors.joining(", "));
		}
	}

	@NotNull
	private static <T extends WithLongId> Map<Long, T> buildMap(Collection<T> source) {
		if (source == null || source.isEmpty()) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(source.stream().collect(Collectors.toMap(WithLongId::getId,
		                                                                            Function.identity(),
		                                                                            (v1, v2) -> { throw new IllegalStateException("Le merger ne devrait pas être appelé !!"); },
		                                                                            LinkedHashMap::new)));
	}

	@Override
	public Map<Long, RegpmEntreprise> getEntreprises() {
		return entreprises;
	}

	@Override
	public Map<Long, RegpmEtablissement> getEtablissements() {
		return etablissements;
	}

	@Override
	public Map<Long, RegpmIndividu> getIndividus() {
		return individus;
	}
}
