package ch.vd.uniregctb.common;

import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;

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
		final TypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == TypeEtatCivil.MARIE || type == TypeEtatCivil.PACS;
	}

	/**
	 * Retourne true si l'état civil représente la valeur SEPARE ou PACS_INTERROMPU.
	 * @param etatCivil
	 * @return
	 */
	public static boolean estSepare(EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final TypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == TypeEtatCivil.SEPARE || type == TypeEtatCivil.PACS_INTERROMPU;
	}

	/**
	 * Retourne true si l'état civil représente la valeur DIVORCE ou PACS_TERMINE.
	 * @param etatCivil
	 * @return
	 */
	public static boolean estDivorce(EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final TypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == TypeEtatCivil.DIVORCE || type == TypeEtatCivil.PACS_TERMINE;
	}

	/**
	 * @return <b>vrai</b> si l'état civil est VEUF; <b>faux</b> autrement.
	 */
	public static boolean estVeuf(EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final TypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == TypeEtatCivil.VEUF || type == TypeEtatCivil.PACS_VEUF;
	}
}
