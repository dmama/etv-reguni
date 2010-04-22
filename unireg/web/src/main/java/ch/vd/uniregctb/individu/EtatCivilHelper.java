package ch.vd.uniregctb.individu;

import ch.vd.registre.civil.model.EnumTypeEtatCivil;

public class EtatCivilHelper {

	public static String getString(EnumTypeEtatCivil typeEtatCivil) {

		if (typeEtatCivil.equals(EnumTypeEtatCivil.CELIBATAIRE))
			return ch.vd.uniregctb.type.EtatCivil.from(EnumTypeEtatCivil.CELIBATAIRE).name();
		else if (typeEtatCivil.equals(EnumTypeEtatCivil.DIVORCE))
			return ch.vd.uniregctb.type.EtatCivil.from(EnumTypeEtatCivil.DIVORCE).name();
		else if (typeEtatCivil.equals(EnumTypeEtatCivil.MARIE))
			return ch.vd.uniregctb.type.EtatCivil.from(EnumTypeEtatCivil.MARIE).name();
		else if (typeEtatCivil.equals(EnumTypeEtatCivil.VEUF))
			return ch.vd.uniregctb.type.EtatCivil.from(EnumTypeEtatCivil.VEUF).name();
		else if (typeEtatCivil.equals(EnumTypeEtatCivil.PACS))
			return ch.vd.uniregctb.type.EtatCivil.from(EnumTypeEtatCivil.PACS).name();
		else if (typeEtatCivil.equals(EnumTypeEtatCivil.PACS_ANNULE))
			return ch.vd.uniregctb.type.EtatCivil.from(EnumTypeEtatCivil.PACS_ANNULE).name();
		else if (typeEtatCivil.equals(EnumTypeEtatCivil.PACS_INTERROMPU))
			return ch.vd.uniregctb.type.EtatCivil.from(EnumTypeEtatCivil.PACS_INTERROMPU).name();
		else if (typeEtatCivil.equals(EnumTypeEtatCivil.SEPARE))
			return ch.vd.uniregctb.type.EtatCivil.from(EnumTypeEtatCivil.SEPARE).name();

		throw new IllegalArgumentException("Etat civil non géré " + typeEtatCivil.getName());
	}
}
