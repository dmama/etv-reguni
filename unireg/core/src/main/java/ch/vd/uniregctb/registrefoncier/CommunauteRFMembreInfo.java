package ch.vd.uniregctb.registrefoncier;

import java.util.Collection;

/**
 * Les informations minimales sur les membres d'une communauté d'un point-de-vue Unireg.
 */
public class CommunauteRFMembreInfo {
	/**
	 * Le nombre de membres de la communauté.
	 */
	private int count;

	/**
	 * Les ids des tiers Unireg qui correspondent aux tiers RF de la communauté (donc, en passant par la table de rapprochement).
	 * <p>
	 * Le nombre d'éléments dans cette collection peut être plus petit que {@link #count}, si ou plusieurs tiers RF ne sont pas rapprochés avec des tiers Unireg.
	 */
	private Collection<Long> ctbIds;

	/**
	 * Les tiers RF des membres de la communauté qui n'ont pas été rapprochés avec des tiers Unireg.
	 */
	private Collection<TiersRF> tiersRF;

	public CommunauteRFMembreInfo(int count, Collection<Long> ctbIds, Collection<TiersRF> tiersRF) {
		this.count = count;
		this.ctbIds = ctbIds;
		this.tiersRF = tiersRF;
	}

	public int getCount() {
		return count;
	}

	public Collection<Long> getCtbIds() {
		return ctbIds;
	}

	public Collection<TiersRF> getTiersRF() {
		return tiersRF;
	}
}
