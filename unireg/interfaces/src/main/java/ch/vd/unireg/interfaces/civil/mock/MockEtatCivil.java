package ch.vd.unireg.interfaces.civil.mock;

import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;

public class MockEtatCivil implements EtatCivil {

	private RegDate dateDebut;
	private TypeEtatCivil typeEtatCivil;

	public MockEtatCivil() {
	}

	public MockEtatCivil(RegDate dateDebut, TypeEtatCivil typeEtatCivil) {
		this.dateDebut = dateDebut;
		this.typeEtatCivil = typeEtatCivil;
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	public void setDateDebut(RegDate dateDebut) {
		this.dateDebut = dateDebut;
	}

	@Override
	public TypeEtatCivil getTypeEtatCivil() {
		return typeEtatCivil;
	}

	public void setTypeEtatCivil(TypeEtatCivil typeEtatCivil) {
		this.typeEtatCivil = typeEtatCivil;
	}

	@Override
	public String toString() {
		return "MockEtatCivil{" +
				"dateDebut=" + dateDebut +
				", typeEtatCivil=" + typeEtatCivil +
				'}';
	}
}
