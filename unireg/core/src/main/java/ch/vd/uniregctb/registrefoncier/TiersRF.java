package ch.vd.uniregctb.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.DateRangeComparator;

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

	private Set<RapprochementRF> rapprochementsRF;

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

	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "RF_TIERS_ID", nullable = false)
	@ForeignKey(name = "FK_RAPPRF_RFTIERS_ID")
	public Set<RapprochementRF> getRapprochementsRF() {
		return rapprochementsRF;
	}

	public void setRapprochementsRF(Set<RapprochementRF> rapprochementsRF) {
		this.rapprochementsRF = rapprochementsRF;
	}

	public void addRapprochementRF(RapprochementRF rapprochement) {
		if (rapprochementsRF == null) {
			rapprochementsRF = new HashSet<>();
		}
		rapprochement.setTiersRF(this);
		rapprochementsRF.add(rapprochement);
	}

	@NotNull
	@Transient
	public List<RapprochementRF> getRapprochementsNonAnnulesTries() {
		if (rapprochementsRF == null) {
			return Collections.emptyList();
		}
		return rapprochementsRF.stream()
				.filter(r -> !r.isAnnule())
				.sorted(DateRangeComparator::compareRanges)
				.collect(Collectors.toList());
	}

	@Override
	public void copyDataTo(AyantDroitRF right) {
		super.copyDataTo(right);
		final TiersRF tiersRight = (TiersRF) right;
		tiersRight.noRF = this.noRF;
		tiersRight.noContribuable = this.noContribuable;
	}
}
