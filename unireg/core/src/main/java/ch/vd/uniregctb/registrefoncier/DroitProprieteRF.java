package ch.vd.uniregctb.registrefoncier;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;

import ch.vd.uniregctb.rf.GenrePropriete;

@Entity
public abstract class DroitProprieteRF extends DroitRF {

	private Fraction part;

	private GenrePropriete regime;

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

	@Column(name = "REGIME_PROPRIETE", nullable = false)
	@Enumerated(EnumType.STRING)
	public GenrePropriete getRegime() {
		return regime;
	}

	public void setRegime(GenrePropriete regime) {
		this.regime = regime;
	}
}
