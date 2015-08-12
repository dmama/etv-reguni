package ch.vd.uniregctb.migration.pm.fusion;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.RegDate;

/**
 * Implémentation de test pour pouvoir tester le comportement des utilisateurs du provider
 * sans avoir à se palucher un fichier CSV d'entrée...
 */
public class MockFusionCommunesProvider implements FusionCommunesProvider {

	/**
	 * Map entre un identifiant OFS de commune et les mutations associées
	 */
	private final Map<Integer, NavigableMap<RegDate, FusionCommunesProviderImpl.DonneesMutation>> communes = new HashMap<>();

	/**
	 * Ajout d'une information dans le mock : à la date de fusion donnée, la commune "avant" a été intégrée dans la commune "référence"
	 */
	public void addCommuneAvant(int noOfsReference, int noOfsCommuneAvant, @NotNull RegDate dateFusion) {
		final NavigableMap<RegDate, FusionCommunesProviderImpl.DonneesMutation> mapReference = getReferenceMap(noOfsReference, true);
		final FusionCommunesProviderImpl.DonneesMutation mut = mapReference.computeIfAbsent(dateFusion, k -> new FusionCommunesProviderImpl.DonneesMutation());
		mut.addOfsAvant(noOfsCommuneAvant);
	}

	/**
	 * Ajout d'une information dans le mock : à la date de disparition donnée, la commune "référence" a été dissoute et intégrée dans la commune "après" dès le lendemain
	 */
	public void addCommuneApres(int noOfsReference, int noOfsCommuneApres, @NotNull RegDate dateDisparition) {
		final NavigableMap<RegDate, FusionCommunesProviderImpl.DonneesMutation> mapReference = getReferenceMap(noOfsReference, true);
		final FusionCommunesProviderImpl.DonneesMutation mut = mapReference.computeIfAbsent(dateDisparition, k -> new FusionCommunesProviderImpl.DonneesMutation());
		mut.addOfsApres(noOfsCommuneApres);
	}

	private NavigableMap<RegDate, FusionCommunesProviderImpl.DonneesMutation> getReferenceMap(int noOfsReference, boolean createIfAbsent) {
		return communes.computeIfAbsent(noOfsReference, k -> mapProvider(!createIfAbsent));
	}

	@Nullable
	private static <K, V> NavigableMap<K, V> mapProvider(boolean noop) {
		return noop ? null : new TreeMap<>();
	}

	@NotNull
	@Override
	public List<Integer> getCommunesAvant(int noOfs, @NotNull RegDate dateFusion) {
		final NavigableMap<RegDate, FusionCommunesProviderImpl.DonneesMutation> mapReference = getReferenceMap(noOfs, false);
		if (mapReference == null || !mapReference.containsKey(dateFusion)) {
			return Collections.emptyList();
		}
		return mapReference.get(dateFusion).getOfsAvant();
	}

	@NotNull
	@Override
	public List<Integer> getCommunesApres(int noOfs, @NotNull RegDate dateDisparition) {
		final NavigableMap<RegDate, FusionCommunesProviderImpl.DonneesMutation> mapReference = getReferenceMap(noOfs, false);
		if (mapReference == null || !mapReference.containsKey(dateDisparition)) {
			return Collections.emptyList();
		}
		return mapReference.get(dateDisparition).getOfsApres();
	}
}
