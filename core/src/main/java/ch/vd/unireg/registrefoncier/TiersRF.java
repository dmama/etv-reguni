package ch.vd.unireg.registrefoncier;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.HashSet;
import java.util.Set;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.tiers.Contribuable;

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

	/**
	 * @return le contribuable rapproché courant; ou <b>null</b> s'il n'y a pas de ctb rapproché.
	 */
	@Transient
	@Nullable
	public Contribuable getCtbRapproche() {
		return rapprochements == null ? null : rapprochements.stream()
				.filter(r -> r.isValidAt(null))
				.findFirst()
				.map(RapprochementRF::getContribuable)
				.orElse(null);
	}

	// configuration hibernate : le tiers RF ne possède pas les rapprochement
	@OneToMany(mappedBy = "tiersRF")
	public Set<RapprochementRF> getRapprochements() {
		return rapprochements;
	}

	public void setRapprochements(Set<RapprochementRF> rapprochements) {
		this.rapprochements = rapprochements;
	}

	public void addRapprochementRF(RapprochementRF rapprochement) {
		if (rapprochements == null) {
			rapprochements = new HashSet<>();
		}
		rapprochement.setTiersRF(this);
		rapprochements.add(rapprochement);
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
