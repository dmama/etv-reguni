package ch.vd.uniregctb.registrefoncier.allegement;

import java.util.Optional;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;

public abstract class AbstractYearRangeView implements DateRange {

	private Integer anneeDebut;
	private Integer anneeFin;

	public AbstractYearRangeView() {
	}

	public AbstractYearRangeView(@NotNull DateRange range) {
		this.anneeDebut = Optional.of(range).map(DateRange::getDateDebut).map(RegDate::year).orElse(null);
		this.anneeFin = Optional.of(range).map(DateRange::getDateFin).map(RegDate::year).orElse(null);
	}

	public AbstractYearRangeView(@NotNull AbstractYearRangeView src) {
		this.anneeDebut = src.anneeDebut;
		this.anneeFin = src.anneeFin;
	}

	@Override
	public RegDate getDateDebut() {
		if (anneeDebut != null) {
			return RegDate.get(anneeDebut, 1, 1);
		}
		else {
			return null;
		}
	}

	@Override
	public RegDate getDateFin() {
		if (anneeFin != null) {
			return RegDate.get(anneeFin, 12, 31);
		}
		else {
			return null;
		}
	}

	public Integer getAnneeDebut() {
		return anneeDebut;
	}

	public void setAnneeDebut(Integer anneeDebut) {
		this.anneeDebut = anneeDebut;
	}

	public Integer getAnneeFin() {
		return anneeFin;
	}

	public void setAnneeFin(Integer anneeFin) {
		this.anneeFin = anneeFin;
	}
}
