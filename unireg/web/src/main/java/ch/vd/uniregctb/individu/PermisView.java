package ch.vd.uniregctb.individu;

import java.io.Serializable;

import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.Permis;

public class PermisView implements DateRange, Serializable {

	private static final long serialVersionUID = -7158428280315582937L;

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

	@SuppressWarnings("unused")
	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	@SuppressWarnings("unused")
	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	@SuppressWarnings("unused")
	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	@SuppressWarnings("unused")
	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	@SuppressWarnings("unused")
	public String getTypePermis() {
		return typePermis;
	}

	@SuppressWarnings("unused")
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
