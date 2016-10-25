package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.jetbrains.annotations.Nullable;

/**
 * Tiers au registre foncier. Un tiers est soit une personne physique, soit une personne morale.
 */
@Entity
public abstract class TiersRF extends AyantDroitRF {

	/**
	 * Numéro public du tiers au registre foncier.
	 */
	private long noRF;

	/**
	 * Numéro de contribuable selon le registre foncier.
	 */
	@Nullable
	private Long noContribuable;

	@Column(name = "NO_RF")
	public long getNoRF() {
		return noRF;
	}

	public void setNoRF(long noRF) {
		this.noRF = noRF;
	}

	@Column(name = "NO_CTB")
	@Nullable
	public Long getNoContribuable() {
		return noContribuable;
	}

	public void setNoContribuable(@Nullable Long noContribuable) {
		this.noContribuable = noContribuable;
	}
}
