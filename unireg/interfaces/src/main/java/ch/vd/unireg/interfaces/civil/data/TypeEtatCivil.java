package ch.vd.unireg.interfaces.civil.data;

import ch.vd.registre.civil.model.EnumTypeEtatCivil;

/**
 * L'état civil à connotation <i>fiscale</i> d'une personne physique. Par rapport à l'état civil officiel, cet état civil possède les états <i>séparé</i> et <i>pacs interrompu</i> en plus.
 */
public enum TypeEtatCivil {

	CELIBATAIRE,
	DIVORCE,
	MARIE,
	PACS,
	/**
	 * Etat civil équivalent à l'état {@link #DIVORCE} mais pour les pacs.
	 */
	PACS_TERMINE,
	/**
	 * Etat civil équivalent à l'état {@link #VEUF} mais pour les pacs.
	 */
	PACS_VEUF,
	/**
	 * Etat civil équivalent à l'état {@link #SEPARE} mais pour les pacs.
	 */
	PACS_INTERROMPU,
	SEPARE,
	VEUF,
	NON_MARIE;

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
			return PACS_TERMINE;
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
			throw new IllegalArgumentException("Type d'état civil inconnu = [" + right.getName() + ']');
		}
	}

}
