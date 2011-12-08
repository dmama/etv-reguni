package ch.vd.uniregctb.interfaces.model.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;

public class MockEtatCivil implements EtatCivil {

	private RegDate dateDebutValidite;

	private Long numeroConjoint;

	private TypeEtatCivil typeEtatCivil;

	@Override
	public RegDate getDateDebutValidite() {
		return dateDebutValidite;
	}

	public void setDateDebutValidite(RegDate dateDebutValidite) {
		this.dateDebutValidite = dateDebutValidite;
	}

	@Override
	public TypeEtatCivil getTypeEtatCivil() {
		return typeEtatCivil;
	}

	public void setTypeEtatCivil(TypeEtatCivil typeEtatCivil) {
		this.typeEtatCivil = typeEtatCivil;
	}
	@Override
	public Long getNumeroConjoint() {
		return numeroConjoint;
	}

	public void setNumeroConjoint(Long numeroConjoint) {
		this.numeroConjoint = numeroConjoint;
	}
}
