package ch.vd.uniregctb.common;

import org.jetbrains.annotations.Nullable;

import ch.vd.uniregctb.interfaces.model.EtatCivil;
import ch.vd.uniregctb.interfaces.model.TypeEtatCivil;

public class EtatCivilHelper {

	/**
	 * @param etatCivil à tester
	 * @return  true si l'état civil représente la valeur MARIE ou PACS
	 */
	public static boolean estMarieOuPacse(@Nullable EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final TypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == TypeEtatCivil.MARIE || type == TypeEtatCivil.PACS;
	}

	/**
	 * @param etatCivil à tester
	 * @return  true si l'état civil représente la valeur SEPARE ou PACS_INTERROMPU.
	 */
	public static boolean estSepare(@Nullable EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final TypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == TypeEtatCivil.SEPARE || type == TypeEtatCivil.PACS_INTERROMPU;
	}

	/**
	 * @param etatCivil à tester
	 * @return  true si l'état civil représente la valeur DIVORCE ou PACS_TERMINE.
	 */
	public static boolean estDivorce(@Nullable EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final TypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == TypeEtatCivil.DIVORCE || type == TypeEtatCivil.PACS_TERMINE;
	}

	/**
	 * @return <b>vrai</b> si l'état civil est VEUF; <b>faux</b> autrement.
	 */
	public static boolean estVeuf(@Nullable EtatCivil etatCivil) {
		if (etatCivil == null)
			return false;
		final TypeEtatCivil type = etatCivil.getTypeEtatCivil();
		return type == TypeEtatCivil.VEUF || type == TypeEtatCivil.PACS_VEUF;
	}
}
