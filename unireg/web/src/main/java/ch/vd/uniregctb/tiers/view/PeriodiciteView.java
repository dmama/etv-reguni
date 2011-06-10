package ch.vd.uniregctb.tiers.view;

import java.util.Date;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.Annulable;
import ch.vd.uniregctb.type.PeriodeDecompte;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

public class PeriodiciteView implements Comparable<PeriodiciteView>, Annulable {

	Long id;

	private PeriodiciteDecompte periodiciteDecompte;

	private PeriodeDecompte periodeDecompte;

	private RegDate dateDebut;

	private RegDate dateFin;

	private Long debiteurId;

	private boolean annule;

	private boolean active;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PeriodiciteDecompte getPeriodiciteDecompte() {
		return periodiciteDecompte;
	}

	public void setPeriodiciteDecompte(PeriodiciteDecompte periodiciteDecompte) {
		this.periodiciteDecompte = periodiciteDecompte;
	}

	public Date getDateDebut() {
		return RegDate.asJavaDate(dateDebut);
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	public Date getDateFin() {
		return  RegDate.asJavaDate(dateFin);
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	public Long getDebiteurId() {
		return debiteurId;
	}

	public void setDebiteurId(Long debiteurId) {
		this.debiteurId = debiteurId;
	}

	@Override
	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public PeriodeDecompte getPeriodeDecompte() {
		return periodeDecompte;
	}

	public void setPeriodeDecompte(PeriodeDecompte periodeDecompte) {
		this.periodeDecompte = periodeDecompte;
	}

	@Override
	public int compareTo(PeriodiciteView periodiciteView) {
		int value = - dateDebut.asJavaDate().compareTo(periodiciteView.getDateDebut());
		return value;
	}

	public boolean isValidAt(RegDate date) {
		return !isAnnule() && RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public boolean isActive() {
		return active;
	}

	public void setActive(boolean active) {
		this.active = active;
	}
}
