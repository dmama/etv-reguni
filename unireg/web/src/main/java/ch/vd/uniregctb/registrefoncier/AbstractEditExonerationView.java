package ch.vd.uniregctb.registrefoncier;

import java.math.BigDecimal;
import java.util.Optional;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.foncier.ExonerationIFONC;

public abstract class AbstractEditExonerationView implements DateRange {

	private Integer pfDebut;
	private Integer pfFin;
	private BigDecimal pourcentageExoneration;

	public AbstractEditExonerationView() {
	}

	public AbstractEditExonerationView(ExonerationIFONC exoneration) {
		this.pfDebut = Optional.ofNullable(exoneration.getDateDebut()).map(RegDate::year).orElse(null);
		this.pfFin = Optional.ofNullable(exoneration.getDateFin()).map(RegDate::year).orElse(null);
		this.pourcentageExoneration = exoneration.getPourcentageExoneration();
	}

	@Override
	public RegDate getDateDebut() {
		return Optional.ofNullable(pfDebut).map(pf -> RegDate.get(pf, 1, 1)).orElse(null);
	}

	@Override
	public RegDate getDateFin() {
		return Optional.ofNullable(pfFin).map(pf -> RegDate.get(pf, 12, 31)).orElse(null);
	}

	public Integer getPfDebut() {
		return pfDebut;
	}

	public void setPfDebut(Integer pfDebut) {
		this.pfDebut = pfDebut;
	}

	public Integer getPfFin() {
		return pfFin;
	}

	public void setPfFin(Integer pfFin) {
		this.pfFin = pfFin;
	}

	public BigDecimal getPourcentageExoneration() {
		return pourcentageExoneration;
	}

	public void setPourcentageExoneration(BigDecimal pourcentageExoneration) {
		this.pourcentageExoneration = pourcentageExoneration;
	}
}
