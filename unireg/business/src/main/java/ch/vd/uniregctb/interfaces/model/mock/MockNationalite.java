package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.Nationalite;
import ch.vd.uniregctb.interfaces.model.Pays;

public class MockNationalite implements Nationalite {

	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private int noSequence;
	private Pays pays;

	public MockNationalite() {
	}

	public MockNationalite(RegDate dateDebutValidite, RegDate dateFinValidite, Pays pays, int noSequence) {
		this.dateDebutValidite = dateDebutValidite;
		this.dateFinValidite = dateFinValidite;
		this.noSequence = noSequence;
		this.pays = pays;
	}

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
	public int getNoSequence() {
		return noSequence;
	}

	public void setNoSequence(int noSequence) {
		this.noSequence = noSequence;
	}

	@Override
	public Pays getPays() {
		return pays;
	}

	public void setPays(Pays pays) {
		this.pays = pays;
	}

}
