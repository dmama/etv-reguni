package ch.vd.uniregctb.entreprise;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Capital;

public class CapitalView implements DateRange {

	private RegDate dateDebut;
	private RegDate dateFin;
	private Long capitalAction;
	private Long capitalLibere;
	private Boolean absenceCapitalLibereNormale;
	private EditionFoscView editionFosc;

	public CapitalView() {
	}

	public CapitalView(Capital capital) {
		this.dateDebut = capital.getDateDebut();
		this.dateFin = capital.getDateFin();
		this.capitalAction = capital.getCapitalAction();
		this.capitalLibere = capital.getCapitalLibere();
		this.editionFosc = new EditionFoscView(capital.getEditionFosc());
	}
	
	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public Long getCapitalAction() {
		return capitalAction;
	}

	public void setCapitalAction(Long capitalAction) {
		this.capitalAction = capitalAction;
	}

	public Long getCapitalLibere() {
		return capitalLibere;
	}

	public void setCapitalLibere(Long capitalLibere) {
		this.capitalLibere = capitalLibere;
	}

	public Boolean getAbsenceCapitalLibereNormale() {
		return absenceCapitalLibereNormale;
	}

	public void setAbsenceCapitalLibereNormale(Boolean absenceCapitalLibereNormale) {
		this.absenceCapitalLibereNormale = absenceCapitalLibereNormale;
	}

	public EditionFoscView getEditionFosc() {
		return editionFosc;
	}

	public void setEditionFosc(EditionFoscView editionFosc) {
		this.editionFosc = editionFosc;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
