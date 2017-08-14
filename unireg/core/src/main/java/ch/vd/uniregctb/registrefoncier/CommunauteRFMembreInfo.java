package ch.vd.uniregctb.registrefoncier;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

import org.jetbrains.annotations.NotNull;

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
	 * Les tiers RF des membres de la communauté qui n'ont pas été rapprochés avec des tiers Unireg.
	 */
	@NotNull
	private final List<TiersRF> tiersRF;

	public CommunauteRFMembreInfo(int count, @NotNull Collection<Long> ctbIds, @NotNull Collection<TiersRF> tiersRF) {
		this.count = count;
		this.ctbIds = new ArrayList<>(ctbIds);
		this.tiersRF = new ArrayList<>(tiersRF);
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
}
