package ch.vd.unireg.tiers.view;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.metier.bouclement.ExerciceCommercial;

public class ExerciceCommercialView implements DateRange {

	private final RegDate dateDebut;
	private final RegDate dateFin;
	private final boolean oneYearLong;
	private final boolean first;
	private final boolean withDI;
	private final boolean tooOldToHaveDI;

	public ExerciceCommercialView(ExerciceCommercial exercice, boolean first, boolean withDI, boolean tooOldToHaveDI) {
		this.dateDebut = exercice.getDateDebut();
		this.dateFin = exercice.getDateFin();
		this.oneYearLong = exercice.getDateDebut().addYears(1).getOneDayBefore() == exercice.getDateFin();
		this.first = first;
		this.withDI = withDI;
		this.tooOldToHaveDI = tooOldToHaveDI;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public boolean isOneYearLong() {
		return oneYearLong;
	}

	public boolean isFirst() {
		return first;
	}

	public boolean isWithDI() {
		return withDI;
	}

	public boolean isTooOldToHaveDI() {
		return tooOldToHaveDI;
	}
}
