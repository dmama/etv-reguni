package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Permis;
import ch.vd.uniregctb.type.TypePermis;

public class MockPermis implements Permis {

	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private RegDate dateAnnulation;
	private int noSequence;
	private TypePermis typePermis;

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	@Override
	public RegDate getDateFinValidite() {
		return dateFinValidite;
	}

	public void setDateFinValidite(RegDate dateFinValidite) {
		this.dateFinValidite = dateFinValidite;
	}

	@Override
	public RegDate getDateAnnulation() {
		return dateAnnulation;
	}

	public void setDateAnnulation(RegDate dateAnnulation) {
		this.dateAnnulation = dateAnnulation;
	}

	@Override
	public int getNoSequence() {
		return noSequence;
	}

	public void setNoSequence(int noSequence) {
		this.noSequence = noSequence;
	}

	@Override
	public TypePermis getTypePermis() {
		return typePermis;
	}

	public void setTypePermis(TypePermis typePermis) {
		this.typePermis = typePermis;
	}

}
