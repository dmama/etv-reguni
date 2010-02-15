package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypePermis;
import ch.vd.uniregctb.interfaces.model.Permis;

public class MockPermis implements Permis {

	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private RegDate dateAnnulation;
	private int noSequence;
	private EnumTypePermis typePermis;

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

	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(RegDate dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public void setNoSequence(int noSequence) {
		this.noSequence = noSequence;
	}

	public EnumTypePermis getTypePermis() {
		return typePermis;
	}

	public void setTypePermis(EnumTypePermis typePermis) {
		this.typePermis = typePermis;
	}

}
