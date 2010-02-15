package ch.vd.uniregctb.interfaces.model.wrapper.apireg;

import org.springframework.util.Assert;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivil;

public class EtatCivilWrapper implements EtatCivil {

	private final int noSequence;
	private final RegDate dateDebut;
	private final EnumTypeEtatCivil type;

	public static EtatCivilWrapper get(ch.vd.apireg.datamodel.EtatCivil target) {
		if (target == null) {
			return null;
		}
		return new EtatCivilWrapper(target);
	}

	private EtatCivilWrapper(ch.vd.apireg.datamodel.EtatCivil target) {
		this.noSequence = target.getId().getNoSequence();
		this.dateDebut = RegDate.get(target.getDaValidite());
		this.type = extractEnum(target);
	}

	public RegDate getDateDebutValidite() {
		return dateDebut;
	}

	public int getNoSequence() {
		return noSequence;
	}

	public EnumTypeEtatCivil getTypeEtatCivil() {
		return type;
	}

	private EnumTypeEtatCivil extractEnum(ch.vd.apireg.datamodel.EtatCivil target) {
		final String code = target.getCodeEtatCivil();
		Assert.isTrue(code.length() == 1);
		final char c = code.charAt(0);

		switch (c) {
		case 'C':
			return EnumTypeEtatCivil.CELIBATAIRE;
		case 'D':
			return EnumTypeEtatCivil.DIVORCE;
		case 'M':
			return EnumTypeEtatCivil.MARIE;
		case 'P':
			return EnumTypeEtatCivil.PACS;
		case 'A':
			return EnumTypeEtatCivil.PACS_ANNULE;
		case 'I':
			return EnumTypeEtatCivil.PACS_INTERROMPU;
		case 'S':
			return EnumTypeEtatCivil.SEPARE;
		case 'V':
			return EnumTypeEtatCivil.VEUF;
		default:
			throw new IllegalArgumentException("Typ de code état civil inconnu = [" + c + "]");
		}
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((dateDebut == null) ? 0 : dateDebut.hashCode());
		result = prime * result + noSequence;
		result = prime * result + ((type == null) ? 0 : type.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		EtatCivilWrapper other = (EtatCivilWrapper) obj;
		if (dateDebut == null) {
			if (other.dateDebut != null)
				return false;
		}
		else if (!dateDebut.equals(other.dateDebut))
			return false;
		if (noSequence != other.noSequence)
			return false;
		if (type == null) {
			if (other.type != null)
				return false;
		}
		else if (!type.equals(other.type))
			return false;
		return true;
	}
}
