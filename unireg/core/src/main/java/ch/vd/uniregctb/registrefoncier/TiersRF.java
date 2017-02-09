package ch.vd.uniregctb.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.Set;

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

	private Set<RapprochementRF> rapprochements;

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

	// configuration hibernate : le tiers RF ne possède pas les rapprochement
	@OneToMany(mappedBy = "tiersRF")
	public Set<RapprochementRF> getRapprochements() {
		return rapprochements;
	}

	public void setRapprochements(Set<RapprochementRF> rapprochements) {
		this.rapprochements = rapprochements;
	}

	@Override
	public void copyDataTo(AyantDroitRF right) {
		super.copyDataTo(right);
		final TiersRF tiersRight = (TiersRF) right;
		if (this.noRF > 0) {    // le numéro RF n'existe pas dans le rechtregister : inutile de le copier s'il n'est pas renseigné
			tiersRight.noRF = this.noRF;
		}
		tiersRight.noContribuable = this.noContribuable;
	}
}
