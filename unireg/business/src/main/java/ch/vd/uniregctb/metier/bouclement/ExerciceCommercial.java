package ch.vd.uniregctb.metier.bouclement;

import java.io.Serializable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class ExerciceCommercial implements DateRange, Serializable {

	private static final long serialVersionUID = -3906110232667675796L;

	private final RegDate dateDebut;
	private final RegDate dateFin;

	public ExerciceCommercial(RegDate dateDebut, RegDate dateFin) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}
}
