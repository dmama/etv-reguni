package ch.vd.uniregctb.etiquette;

import java.util.function.Function;

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
}
