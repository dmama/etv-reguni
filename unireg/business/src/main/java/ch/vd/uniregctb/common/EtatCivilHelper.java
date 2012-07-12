package ch.vd.uniregctb.common;

import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.civil.data.EtatCivil;
import ch.vd.unireg.interfaces.civil.data.TypeEtatCivil;

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

	public static ch.vd.uniregctb.type.EtatCivil civil2core(TypeEtatCivil etatCivil) {
		if (etatCivil == null) {
			return null;
		}
		switch (etatCivil) {
		case CELIBATAIRE:
			return ch.vd.uniregctb.type.EtatCivil.CELIBATAIRE;
		case DIVORCE:
			return ch.vd.uniregctb.type.EtatCivil.DIVORCE;
		case MARIE:
			return ch.vd.uniregctb.type.EtatCivil.MARIE;
		case PACS:
			return ch.vd.uniregctb.type.EtatCivil.LIE_PARTENARIAT_ENREGISTRE;
		case PACS_TERMINE:
			return ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT;
		case PACS_VEUF:
			return ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_DECES;
		case PACS_INTERROMPU:
			return ch.vd.uniregctb.type.EtatCivil.PARTENARIAT_DISSOUS_JUDICIAIREMENT;
		case SEPARE:
			return ch.vd.uniregctb.type.EtatCivil.SEPARE;
		case VEUF:
			return ch.vd.uniregctb.type.EtatCivil.VEUF;
		case NON_MARIE:
			return ch.vd.uniregctb.type.EtatCivil.NON_MARIE;
		default:
			throw new IllegalArgumentException("Type d'état-civil inconnu = [" + etatCivil + "]");
		}
	}
}
