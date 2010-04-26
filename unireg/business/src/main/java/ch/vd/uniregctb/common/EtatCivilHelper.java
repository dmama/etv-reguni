package ch.vd.uniregctb.common;

import ch.vd.registre.civil.model.EnumTypeEtatCivil;
import ch.vd.uniregctb.interfaces.model.EtatCivil;

/**
 *
 * @author Pavel BLANCO
 *
 */
public class EtatCivilHelper {

	/**
	 * Retourne true si l'état civil représente la valeur MARIE ou PACS
	 * @param etatCivil
	 * @return
	 */
	public static boolean estMarieOuPacse(EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final EnumTypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == EnumTypeEtatCivil.MARIE || type == EnumTypeEtatCivil.PACS;
	}

	/**
	 * Retourne true si l'état civil représente la valeur MARIE ou LIE_PARTENARIAT_ENREGISTRE
	 * @param etatCivil
	 * @return
	 */
	public static boolean estMarieOuPacse(ch.vd.uniregctb.type.EtatCivil etatCivil) {
		return ch.vd.uniregctb.type.EtatCivil.MARIE.equals(etatCivil) || ch.vd.uniregctb.type.EtatCivil.LIE_PARTENARIAT_ENREGISTRE.equals(etatCivil);
	}
	/**
	 * Retourne true si l'état civil représente la valeur SEPARE ou PACS_INTERROMPU.
	 * @param etatCivil
	 * @return
	 */
	public static boolean estSepare(EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final EnumTypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == EnumTypeEtatCivil.SEPARE || type == EnumTypeEtatCivil.PACS_INTERROMPU;
	}

	/**
	 * Retourne true si l'état civil représente la valeur DIVORCE ou PACS_ANNULE.
	 * @param etatCivil
	 * @return
	 */
	public static boolean estDivorce(EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final EnumTypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == EnumTypeEtatCivil.DIVORCE || type == EnumTypeEtatCivil.PACS_ANNULE;
	}

	/**
	 * @return <b>vrai</b> si l'état civil est VEUF; <b>faux</b> autrement.
	 */
	public static boolean estVeuf(EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final EnumTypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == EnumTypeEtatCivil.VEUF;
	}
}
