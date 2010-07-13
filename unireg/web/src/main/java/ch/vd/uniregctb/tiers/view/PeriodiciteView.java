package ch.vd.uniregctb.tiers.view;

import java.util.Date;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.type.PeriodiciteDecompte;

public class PeriodiciteView implements Comparable<PeriodiciteView> {

	Long id;

	PeriodiciteDecompte periodiciteDecompte;

	RegDate dateDebut;

	RegDate dateFin;

	Long debiteurId;

	private boolean annule;

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

	public boolean isAnnule() {
		return annule;
	}

	public void setAnnule(boolean annule) {
		this.annule = annule;
	}

	public int compareTo(PeriodiciteView periodiciteView) {
		int value = - dateDebut.asJavaDate().compareTo(periodiciteView.getDateDebut());
		return value;
	}
}
