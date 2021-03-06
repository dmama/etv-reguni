package ch.vd.unireg.registrefoncier.allegement;

import java.math.BigDecimal;

import ch.vd.unireg.foncier.ExonerationIFONC;

public abstract class AbstractEditExonerationView extends AbstractYearRangeView {

	private BigDecimal pourcentageExoneration;

	public AbstractEditExonerationView() {
		super();
	}

	public AbstractEditExonerationView(ExonerationIFONC exoneration) {
		super(exoneration);
		this.pourcentageExoneration = exoneration.getPourcentageExoneration();
	}

	public BigDecimal getPourcentageExoneration() {
		return pourcentageExoneration;
	}

	public void setPourcentageExoneration(BigDecimal pourcentageExoneration) {
		this.pourcentageExoneration = pourcentageExoneration;
	}
}
