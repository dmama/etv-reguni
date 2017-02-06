package ch.vd.uniregctb.foncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.math.BigDecimal;

@Entity
@DiscriminatorValue(value = "ExonerationIFONC")
public class ExonerationIFONC extends AllegementFoncier {

	/**
	 * Pourcentage d'exonération pour l'impôt foncier (0-100, 2 décimales)
	 */
	private BigDecimal pourcentageExoneration;

	public ExonerationIFONC() {
	}

	@Transient
	@Override
	public TypeImpot getTypeImpot() {
		return TypeImpot.IFONC;
	}

	@Column(name = "IFONC_POURCENT_EXO", precision = 5, scale = 2)
	public BigDecimal getPourcentageExoneration() {
		return pourcentageExoneration;
	}

	public void setPourcentageExoneration(BigDecimal pourcentageExoneration) {
		this.pourcentageExoneration = pourcentageExoneration;
	}
}
