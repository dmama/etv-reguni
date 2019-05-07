package ch.vd.unireg.registrefoncier;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

/**
 * Représente un immeuble qui bénéficie d'un ou plusieurs droits sur d'autres immeubles.
 */
@Entity
@DiscriminatorValue("Immeuble")
public class ImmeubleBeneficiaireRF extends AyantDroitRF {

	private ImmeubleRF immeuble;

	@OneToOne(cascade = CascadeType.ALL)
	@JoinColumn(name = "IMMEUBLE_ID", foreignKey = @ForeignKey(name = "FK_IMM_BENE_RF_IMMEUBLE_ID"))
	public ImmeubleRF getImmeuble() {
		return immeuble;
	}

	public void setImmeuble(ImmeubleRF immeuble) {
		this.immeuble = immeuble;
	}
}
