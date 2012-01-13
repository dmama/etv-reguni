package ch.vd.uniregctb.individu;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.Permis;

public class PermisView implements DateRange {

	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private String typePermis;
	private boolean annule;

	public PermisView(Permis permis) {
		this.typePermis = permis.getTypePermis() == null ? null : permis.getTypePermis().name();
		this.dateDebutValidite = permis.getDateDebut();
		this.dateFinValidite = permis.getDateFin();
		this.annule = permis.getDateAnnulation() != null;
	}

	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}
	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}
	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}
	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}
	public String getTypePermis() {
		return typePermis;
	}
	public void setTypePermis(String typePermis) {
		this.typePermis = typePermis;
	}
	@Override
	public RegDate getDateDebut() {
		return dateDebutValidite;
	}
	@Override
	public RegDate getDateFin() {
		return dateFinValidite;
	}
	public boolean isAnnule() {
		return annule;
	}
	public void setAnnule(boolean annule) {
		this.annule = annule;
	}
	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebutValidite, dateFinValidite, NullDateBehavior.LATEST);
	}

}
