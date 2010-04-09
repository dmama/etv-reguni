package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivil;

public class EtatCivilWrapper implements EtatCivil {

	private final RegDate dateDebut;
	private int noSequence;
	private EnumTypeEtatCivil typeEtatCivil;

	public static EtatCivilWrapper get(ch.vd.registre.civil.model.EtatCivil target) {
		if (target == null) {
			return null;
		}
		return new EtatCivilWrapper(target);
	}

	private EtatCivilWrapper(ch.vd.registre.civil.model.EtatCivil target) {
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
		this.noSequence = target.getNoSequence();
		this.typeEtatCivil = target.getTypeEtatCivil();
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public EnumTypeEtatCivil getTypeEtatCivil() {
		return typeEtatCivil;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;

		final EtatCivilWrapper that = (EtatCivilWrapper) o;

		if (noSequence != that.noSequence) return false;
		if (dateDebut != null ? !dateDebut.equals(that.dateDebut) : that.dateDebut != null) return false;
		if (typeEtatCivil != null ? !typeEtatCivil.equals(that.typeEtatCivil) : that.typeEtatCivil != null) return false;

		return true;
	}

	@Override
	public int hashCode() {
		int result = dateDebut != null ? dateDebut.hashCode() : 0;
		result = 31 * result + noSequence;
		result = 31 * result + (typeEtatCivil != null ? typeEtatCivil.hashCode() : 0);
		return result;
	}
}
