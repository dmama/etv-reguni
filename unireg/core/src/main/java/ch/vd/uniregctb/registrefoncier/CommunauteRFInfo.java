package ch.vd.uniregctb.registrefoncier;

import java.util.Collection;

/**
 * Les informations minimales sur les membres d'une communauté d'un point-de-vue Unireg.
 */
public class CommunauteRFInfo {
	/**
	 * Le nombre de membres de la communauté.
	 */
	private int memberCount;

	/**
	 * Les ids des tiers Unireg qui correspondent aux tiers RF de la communauté (donc, en passant par la table de rapprochement).
	 * <p>
	 * Le nombre d'éléments dans cette collection peut être plus petit que {@link #memberCount}, si ou plusieurs tiers RF ne sont pas rapprochés avec des tiers Unireg.
	 */
	private Collection<Integer> memberIds;

	public CommunauteRFInfo(int memberCount, Collection<Integer> memberIds) {
		this.memberCount = memberCount;
		this.memberIds = memberIds;
	}

	public int getMemberCount() {
		return memberCount;
	}

	public Collection<Integer> getMemberIds() {
		return memberIds;
	}
}
