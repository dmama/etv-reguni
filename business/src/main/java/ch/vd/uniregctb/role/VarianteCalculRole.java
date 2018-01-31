package ch.vd.uniregctb.role;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Interface de base des variantes de calcul du rôle
 * @param <T> type de contribuable concerné
 * @param <R> type du rapport généré
 */
public interface VarianteCalculRole<T extends Contribuable, R extends RoleResults<R>> {

	/**
	 * @return un nouveau rapport
	 */
	@NotNull
	R buildRapport();

	/**
	 * @param annee année des rôles
	 * @param ofsCommune [optionnel] limitation sur les communes recherchées
	 * @return la liste des identifiants des contribuables concernés par le rôle à calculer
	 */
	@NotNull
	List<Long> getIdsContribuables(int annee, @Nullable Set<Integer> ofsCommune);

	/**
	 * @param annee année des rôles
	 * @param contribuables ensemble de contribuables à analyser
	 * @return répartition des contribuables par commune de référence pour le rôle (la clé <code>null</code> correspond aux contribuables qui ne font pas partie du rôle de l'année donnée)
	 */
	@NotNull
	Map<Integer, List<T>> dispatch(int annee, Collection<T> contribuables);

	/**
	 * Procède à l'intégration d'un contribuable dans le rôle de la commune indiquée (si <code>null</code>, on devra
	 * considérer ce contribuable comme "ignoré")
	 * @param rapport le rapport de l'extraction
	 * @param contribuable le contribuable à intégrer
	 * @param ofsCommuneRole [optionnel] la commune de référence du contribuable pour le rôle (contribuable à ignorer si <code>null</code>)
	 */
	void compile(R rapport, T contribuable, @Nullable Integer ofsCommuneRole) throws CalculRoleException;
}
