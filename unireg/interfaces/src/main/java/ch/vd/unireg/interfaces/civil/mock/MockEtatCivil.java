package ch.vd.unireg.interfaces.civil.mock;

import org.jetbrains.annotations.Nullable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;

public class MockEtatCivil implements EtatCivil {

	private RegDate dateDebut;
	private RegDate dateFin;
	private TypeEtatCivil typeEtatCivil;

	public MockEtatCivil() {
	}

	public MockEtatCivil(RegDate dateDebut, @Nullable RegDate dateFin, TypeEtatCivil typeEtatCivil) {
		this.dateDebut = dateDebut;
		this.dateFin = dateFin;
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
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public TypeEtatCivil getTypeEtatCivil() {
		return typeEtatCivil;
	}

	public void setTypeEtatCivil(TypeEtatCivil typeEtatCivil) {
		this.typeEtatCivil = typeEtatCivil;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}
}
