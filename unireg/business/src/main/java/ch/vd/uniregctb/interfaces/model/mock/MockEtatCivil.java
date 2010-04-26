package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivil;

public class MockEtatCivil implements EtatCivil {

	private RegDate dateDebutValidite;

	private int noSequence;

	private Long numeroConjoint;

	private EnumTypeEtatCivil typeEtatCivil;

	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public void setNoSequence(int noSequence) {
		this.noSequence = noSequence;
	}

	public EnumTypeEtatCivil getTypeEtatCivil() {
		return typeEtatCivil;
	}

	public void setTypeEtatCivil(EnumTypeEtatCivil typeEtatCivil) {
		this.typeEtatCivil = typeEtatCivil;
	}
	public Long getNumeroConjoint() {
		return numeroConjoint;
	}

	public void setNumeroConjoint(Long numeroConjoint) {
		this.numeroConjoint = numeroConjoint;
	}
}
