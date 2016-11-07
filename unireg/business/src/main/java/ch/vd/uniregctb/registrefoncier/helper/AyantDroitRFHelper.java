package ch.vd.uniregctb.registrefoncier.helper;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.GeburtsDatum;
import ch.vd.capitastra.grundstueck.JuristischePersonUnterart;
import ch.vd.capitastra.grundstueck.JuristischePersonstamm;
import ch.vd.capitastra.grundstueck.NatuerlichePersonstamm;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public abstract class AyantDroitRFHelper {

	private AyantDroitRFHelper() {
	}

	/**
	 * @return <b>vrai</b> si les deux ayant-droits spécifiés possèdent les mêmes données; <b>faux</b> autrement.
	 */
	public static boolean dataEquals(@NotNull AyantDroitRF left, @NotNull Personstamm right) {
		if (!left.getIdRF().equals(right.getPersonstammID())) {
			// erreur de programmation, on ne devrait jamais comparer deux ayant-droit avec des IDRef différents
			throw new ProgrammingException();
		}

		// [blindage] les valeurs suivantes ne doivent jamais changer (le modèle est construit sur ce prédicat)
		if (left instanceof PersonnePhysiqueRF && !(right instanceof NatuerlichePersonstamm)) {
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		if (left instanceof PersonneMoraleRF && (!(right instanceof JuristischePersonstamm) ||
				((JuristischePersonstamm) right).getUnterart() == JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT)) {
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		if (left instanceof CollectivitePubliqueRF && (!(right instanceof JuristischePersonstamm) ||
				((JuristischePersonstamm) right).getUnterart() != JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT)) {
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		if (left instanceof CommunauteRF) {
			// les communautés RF sont renseignées à partir des droits, on ne devrait pas en recevoir par ce canal
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		// [/blindage]

		if (left instanceof PersonnePhysiqueRF) {
			return dataEquals((PersonnePhysiqueRF) left, (NatuerlichePersonstamm) right);
		}
		else if (left instanceof PersonneMoraleRF) {
			return dataEquals((PersonneMoraleRF) left, (JuristischePersonstamm) right);
		}
		else if (left instanceof CollectivitePubliqueRF) {
			return dataEquals((CollectivitePubliqueRF) left, (JuristischePersonstamm) right);
		}
		else {
			throw new IllegalArgumentException("Type de tiers inconnu = [" + left.getClass() + "]");
		}
	}

	public static boolean dataEquals(@NotNull PersonnePhysiqueRF left, @NotNull NatuerlichePersonstamm right) {
		return left.getNoRF() == right.getNoRF() &&
				(Objects.equals(left.getNoContribuable(), getNoContribuable(right))) &&
				Objects.equals(left.getNom(), right.getName()) &&
				Objects.equals(left.getPrenom(), right.getVorname()) &&
				left.getDateNaissance() == getRegDate(right.getGeburtsdatum());
	}

	public static boolean dataEquals(@NotNull PersonneMoraleRF left, @NotNull JuristischePersonstamm right) {
		return left.getNoRF() == right.getNoRF() &&
				(Objects.equals(left.getNoContribuable(), getNoContribuable(right))) &&
				Objects.equals(left.getRaisonSociale(), right.getName());
	}

	public static boolean dataEquals(@NotNull CollectivitePubliqueRF left, @NotNull JuristischePersonstamm right) {
		return left.getNoRF() == right.getNoRF() &&
				(Objects.equals(left.getNoContribuable(), getNoContribuable(right))) &&
				Objects.equals(left.getRaisonSociale(), right.getName());
	}

	public static AyantDroitRFKey newAyantDroitKey(@NotNull Personstamm person) {
		return new AyantDroitRFKey(person.getPersonstammID());
	}

	private static RegDate getRegDate(@Nullable GeburtsDatum geburtsdatum) {
		if (geburtsdatum == null) {
			return null;
		}
		return RegDateHelper.get(geburtsdatum.getJahr(), geburtsdatum.getMonat(), geburtsdatum.getTag());
	}

	@Nullable
	private static Long getNoContribuable(@NotNull Personstamm right) {
		Long no = right.getNrACI();
		if (no == null && right instanceof NatuerlichePersonstamm) {
			no = ((NatuerlichePersonstamm)right).getNrIROLE();
		}
		return no;
	}
}