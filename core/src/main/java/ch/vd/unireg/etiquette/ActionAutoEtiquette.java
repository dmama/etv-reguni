package ch.vd.unireg.etiquette;

import java.util.Optional;
import java.util.function.Function;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.RegDate;

public class ActionAutoEtiquette {

	private Function<RegDate, RegDate> dateDebut;
	private Function<RegDate, RegDate> dateFin;

	public ActionAutoEtiquette(Function<RegDate, RegDate> dateDebut, Function<RegDate, RegDate> dateFin) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
	}

	public Function<RegDate, RegDate> getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(Function<RegDate, RegDate> dateDebut) {
		this.dateDebut = dateDebut;
	}

	public Function<RegDate, RegDate> getDateFin() {
		return dateFin;
	}

	public void setDateFin(Function<RegDate, RegDate> dateFin) {
		this.dateFin = dateFin;
	}

	/**
	 * @param date une date de référence
	 * @return un range construit à partir d'une date de référence et des fonctions de génération de la date de début et de la date de fin
	 */
	public DateRange rangeFromDate(RegDate date) {
		final RegDate debut = Optional.ofNullable(dateDebut).map(f -> f.apply(date)).orElse(null);
		final RegDate fin = Optional.ofNullable(dateFin).map(f -> f.apply(date)).orElse(null);
		return new DateRangeHelper.Range(debut, fin);
	}
}
