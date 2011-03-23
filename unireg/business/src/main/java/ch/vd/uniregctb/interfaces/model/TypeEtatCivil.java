package ch.vd.uniregctb.interfaces.model;

import ch.vd.registre.civil.model.EnumTypeEtatCivil;

public enum TypeEtatCivil {

	CELIBATAIRE(ch.vd.uniregctb.type.EtatCivil.CELIBATAIRE),
	DIVORCE(ch.vd.uniregctb.type.EtatCivil.DIVORCE),
	MARIE(ch.vd.uniregctb.type.EtatCivil.MARIE),
	PACS(ch.vd.uniregctb.type.EtatCivil.LIE_PARTENARIAT_ENREGISTRE),
	PACS_ANNULE(ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT),
	PACS_INTERROMPU(ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT),
	SEPARE(ch.vd.uniregctb.type.EtatCivil.SEPARE),
	VEUF(ch.vd.uniregctb.type.EtatCivil.VEUF);

	private ch.vd.uniregctb.type.EtatCivil core;

	TypeEtatCivil(ch.vd.uniregctb.type.EtatCivil core) {
		this.core = core;
	}

	public static TypeEtatCivil get(ch.vd.registre.civil.model.EnumTypeEtatCivil right) {
		if (right == null) {
			return null;
		}
		if (right == EnumTypeEtatCivil.CELIBATAIRE) {
			return CELIBATAIRE;
		}
		else if (right == EnumTypeEtatCivil.DIVORCE) {
			return DIVORCE;
		}
		else if (right == EnumTypeEtatCivil.MARIE) {
			return MARIE;
		}
		else if (right == EnumTypeEtatCivil.PACS) {
			return PACS;
		}
		else if (right == EnumTypeEtatCivil.PACS_ANNULE) {
			return PACS_ANNULE;
		}
		else if (right == EnumTypeEtatCivil.PACS_INTERROMPU) {
			return PACS_INTERROMPU;
		}
		else if (right == EnumTypeEtatCivil.SEPARE) {
			return SEPARE;
		}
		else if (right == EnumTypeEtatCivil.VEUF) {
			return VEUF;
		}
		else {
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + right.getName()+"]");
		}
	}

	public ch.vd.uniregctb.type.EtatCivil asCore() {
		return core;
	}
}
