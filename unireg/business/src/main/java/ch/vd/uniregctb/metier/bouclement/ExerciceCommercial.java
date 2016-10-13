package ch.vd.uniregctb.metier.bouclement;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

public class ExerciceCommercial implements DateRange, Serializable {

	private static final long serialVersionUID = 4684660603465556391L;

	private final RegDate dateDebut;
	private final RegDate dateFin;

	public ExerciceCommercial(RegDate dateDebut, RegDate dateFin) {
		if (dateDebut == null || dateFin == null) {
			throw new IllegalArgumentException("Les exercices commerciaux sont forcément bornés : " + DateRangeHelper.toDisplayString(dateDebut, dateFin));
		}
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	@NotNull
	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@NotNull
	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	@Override
	public String toString() {
		return "ExerciceCommercial{" +
				"dateDebut=" + dateDebut +
				", dateFin=" + dateFin +
				'}';
	}
}
