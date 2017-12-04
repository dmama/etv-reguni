package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.tiers.CommunauteHeritiers;
import ch.vd.uniregctb.tiers.Heritage;

/**
 * Les informations minimales sur les membres d'une communauté d'un point-de-vue Unireg.
 */
public class CommunauteRFMembreInfo {
	/**
	 * Le nombre de membres de la communauté.
	 */
	private final int count;

	/**
	 * Les ids des tiers Unireg qui correspondent aux tiers RF de la communauté (donc, en passant par la table de rapprochement).
	 * <p>
	 * Le nombre d'éléments dans cette collection peut être plus petit que {@link #count}, si ou plusieurs tiers RF ne sont pas rapprochés avec des tiers Unireg.
	 */
	@NotNull
	private final List<Long> ctbIds;

	/**
	 * L'historique des principaux de la communauté
	 */
	private final List<CommunauteRFPrincipalInfo> principaux;

	/**
	 * Les tiers RF des membres de la communauté qui n'ont pas été rapprochés avec des tiers Unireg.
	 */
	@NotNull
	private final List<TiersRF> tiersRF;

	public CommunauteRFMembreInfo(int count, @NotNull Collection<Long> ctbIds, @NotNull Collection<TiersRF> tiersRF) {
		this.count = count;
		this.ctbIds = new ArrayList<>(ctbIds);
		this.tiersRF = new ArrayList<>(tiersRF);
		this.principaux = new ArrayList<>();
	}

	/**
	 * Remplace les membres défunts par leurs héritiers en fonction des communautés d'héritiers spécifiées.
	 *
	 * @param communautesHeritiers les communautés d'héritiers qui concernent les membres de la communauté
	 * @return une nouvelle instance des infos de la communauté
	 */
	@NotNull
	public CommunauteRFMembreInfo apply(@NotNull Map<Long, CommunauteHeritiers> communautesHeritiers) {

		int count = this.count;
		final Set<Long> membresIds = new HashSet<>(this.ctbIds);

		for (Map.Entry<Long, CommunauteHeritiers> entry : communautesHeritiers.entrySet()) {
			// on supprime le défunt
			if (membresIds.remove(entry.getKey())) {
				count--;
				// on ajoute les héritiers
				final Set<Long> heritiers = entry.getValue().getLiensHeritage().stream()
						.map(Heritage::getSujetId)
						.collect(Collectors.toSet());
				membresIds.addAll(heritiers);
				count += heritiers.size();
			}
		}

		return new CommunauteRFMembreInfo(count, membresIds, this.tiersRF);
	}

	/**
	 * Trie les membres de la communautés (identifiés par leurs ids de contribuables, les tiers RF non-rapprochés ne sont pas triés).
	 *
	 * @param comparator le comparateur à utiliser pour le tri.
	 */
	public void sortMembers(Comparator<Long> comparator) {
		this.ctbIds.sort(comparator);
	}

	public int getCount() {
		return count;
	}

	@NotNull
	public List<Long> getCtbIds() {
		return ctbIds;
	}

	@NotNull
	public List<TiersRF> getTiersRF() {
		return tiersRF;
	}

	public void setPrincipaux(List<CommunauteRFPrincipalInfo> principaux) {
		this.principaux.addAll(principaux);
	}

	@NotNull
	public List<CommunauteRFPrincipalInfo> getPrincipaux() {
		return principaux;
	}
}
