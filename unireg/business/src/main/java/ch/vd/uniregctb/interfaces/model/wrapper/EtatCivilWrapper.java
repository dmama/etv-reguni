package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivil;

public class EtatCivilWrapper implements EtatCivil {

	private final ch.vd.registre.civil.model.EtatCivil target;
	private final RegDate dateDebut;

	public static EtatCivilWrapper get(ch.vd.registre.civil.model.EtatCivil target) {
		if (target == null) {
			return null;
		}
		return new EtatCivilWrapper(target);
	}

	private EtatCivilWrapper(ch.vd.registre.civil.model.EtatCivil target) {
		this.target = target;
		this.dateDebut = RegDate.get(target.getDateDebutValidite());
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public int getNoSequence() {
		return target.getNoSequence();
	}

	public EnumTypeEtatCivil getTypeEtatCivil() {
		return target.getTypeEtatCivil();
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final EtatCivilWrapper other = (EtatCivilWrapper) obj;
		if (dateDebut == null) {
			if (other.dateDebut != null)
				return false;
		}
		else if (!dateDebut.equals(other.dateDebut))
			return false;
		if (target == null) {
			if (other.target != null)
				return false;
		}
		else  {
			if (target.getNoSequence() != other.target.getNoSequence())
				return false;
			else if (!target.getTypeEtatCivil().equals(other.target.getTypeEtatCivil()))
				return false;
		}
		return true;
	}


}
