package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Transient;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

import org.hibernate.annotations.ForeignKey;
import org.jetbrains.annotations.NotNull;

import ch.vd.uniregctb.common.LengthConstants;
import ch.vd.uniregctb.rf.GenrePropriete;

@Entity
public abstract class DroitProprieteRF extends DroitRF {

	private Fraction part;

	private GenrePropriete regime;

	/**
	 * Les raison d'acquisition du droit.
	 */
	private Set<RaisonAcquisitionRF> raisonsAcquisition;

	@AttributeOverrides({
			@AttributeOverride(name = "numerateur", column = @Column(name = "PART_PROP_NUM")),
			@AttributeOverride(name = "denominateur", column = @Column(name = "PART_PROP_DENOM"))
	})
	public Fraction getPart() {
		return part;
	}

	public void setPart(Fraction part) {
		this.part = part;
	}

	@Column(name = "REGIME_PROPRIETE", length = LengthConstants.RF_GENRE_PROPRIETE)
	@Enumerated(EnumType.STRING)
	public GenrePropriete getRegime() {
		return regime;
	}

	public void setRegime(GenrePropriete regime) {
		this.regime = regime;
	}

	// configuration hibernate : le droit possède les raison d'acquisition
	@OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
	@JoinColumn(name = "DROIT_ID", nullable = false)
	@ForeignKey(name = "FK_RAISON_ACQ_RF_DROIT_ID")
	public Set<RaisonAcquisitionRF> getRaisonsAcquisition() {
		return raisonsAcquisition;
	}

	public void setRaisonsAcquisition(Set<RaisonAcquisitionRF> raisonsAcquisition) {
		this.raisonsAcquisition = raisonsAcquisition;
	}

	public void addRaisonAcquisition(RaisonAcquisitionRF raison) {
		if (raisonsAcquisition == null) {
			raisonsAcquisition = new HashSet<>();
		}
		raisonsAcquisition.add(raison);
	}

	public boolean removeRaisonAcquisition(RaisonAcquisitionRF raison) {
		return raisonsAcquisition != null && raisonsAcquisition.remove(raison);
	}

	@NotNull
	@Transient
	@Override
	public TypeDroit getTypeDroit() {
		return TypeDroit.DROIT_PROPRIETE;
	}

	/**
	 * Calcule la date de début métier et le motif d'acquisition à partir de l'historique des raisons d'acquisition.
	 */
	public void calculateDateEtMotifDebut() {
		if (raisonsAcquisition == null || raisonsAcquisition.isEmpty()) {
			this.setDateDebutMetier(null);
			this.setMotifDebut(null);
		}
		else {
			final RaisonAcquisitionRF first = raisonsAcquisition.stream()
					.min(Comparator.naturalOrder())
					.orElse(null);
			if (first == null) {
				this.setDateDebutMetier(null);
				this.setMotifDebut(null);
			}
			else {
				this.setDateDebutMetier(first.getDateAcquisition());
				this.setMotifDebut(first.getMotifAcquisition());
			}
		}
	}
}
