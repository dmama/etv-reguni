package ch.vd.unireg.foncier;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Transient;
import java.math.BigDecimal;

import ch.vd.unireg.common.Duplicable;

@Entity
@DiscriminatorValue(value = "ExonerationIFONC")
public class ExonerationIFONC extends AllegementFoncier implements Duplicable<ExonerationIFONC> {

	/**
	 * Pourcentage d'exonération pour l'impôt foncier (0-100, 2 décimales)
	 */
	private BigDecimal pourcentageExoneration;

	public ExonerationIFONC() {
	}

	private ExonerationIFONC(ExonerationIFONC src) {
		super(src);
		this.pourcentageExoneration = src.pourcentageExoneration;
	}

	@Transient
	@Override
	public ExonerationIFONC duplicate() {
		return new ExonerationIFONC(this);
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
