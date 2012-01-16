package ch.vd.uniregctb.interfaces.model.impl;

import java.io.Serializable;

import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;

public class EtatCivilImpl implements EtatCivil, Serializable {

	private static final long serialVersionUID = 3282374011657120967L;
	
	private final RegDate dateDebut;
	private RegDate dateFin;
	private final int noSequence;
	private final TypeEtatCivil typeEtatCivil;
	private final Long numeroConjoint;

	public static EtatCivilImpl get(ch.vd.registre.civil.model.EtatCivil target) {
		if (target == null) {
			return null;
		}
		return new EtatCivilImpl(target);
	}

	private EtatCivilImpl(ch.vd.registre.civil.model.EtatCivil target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.noSequence = target.getNoSequence();
		this.typeEtatCivil = TypeEtatCivil.get(target.getTypeEtatCivil());
		this.numeroConjoint = ((ch.vd.registre.civil.model.impl.EtatCivilImpl)target).getNoTechniqueConjoint(); // cast : hack en attendant la résolution de [INTER-158]
	}

	@Override
	public RegDate getDateDebut() {
		return dateDebut;
	}

	@Override
	public RegDate getDateFin() {
		return dateFin;
	}

	public void setDateFin(RegDate dateFin) {
		this.dateFin = dateFin;
	}

	@Override
	public boolean isValidAt(RegDate date) {
		return RegDateHelper.isBetween(date, dateDebut, dateFin, NullDateBehavior.LATEST);
	}

	public int getNoSequence() {
		return noSequence;
	}

	@Override
	public TypeEtatCivil getTypeEtatCivil() {
		return typeEtatCivil;
	}

	@Override
	public Long getNumeroConjoint() {
		return numeroConjoint;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final EtatCivilImpl that = (EtatCivilImpl) o;

		if (noSequence != that.noSequence) return false;
		if (dateDebut != null ? !dateDebut.equals(that.dateDebut) : that.dateDebut != null) return false;
		if (numeroConjoint != null ? !numeroConjoint.equals(that.numeroConjoint) : that.numeroConjoint != null) return false;
		if (typeEtatCivil != null ? typeEtatCivil != that.typeEtatCivil : that.typeEtatCivil != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = dateDebut != null ? dateDebut.hashCode() : 0;
		result = 31 * result + noSequence;
		result = 31 * result + (typeEtatCivil != null ? typeEtatCivil.hashCode() : 0);
		result = 31 * result + (numeroConjoint != null ? numeroConjoint.hashCode() : 0);
		return result;
	}
}
