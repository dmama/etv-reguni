package ch.vd.unireg.interfaces.civil.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.interfaces.civil.data.Pays;

public class MockNationalite implements Nationalite {

	private RegDate dateDebutValidite;
	private RegDate dateFinValidite;
	private Pays pays;

	public MockNationalite() {
	}

	public MockNationalite(RegDate dateDebutValidite, RegDate dateFinValidite, Pays pays, int noSequence) {
		this.dateDebutValidite = dateDebutValidite;
		this.dateFinValidite = dateFinValidite;
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
	public Pays getPays() {
		return pays;
	}

	public void setPays(Pays pays) {
		this.pays = pays;
	}

}
