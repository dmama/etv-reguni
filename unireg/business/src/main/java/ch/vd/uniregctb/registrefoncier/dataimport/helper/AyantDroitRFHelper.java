package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.GeburtsDatum;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GemeinschaftsArt;
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
import ch.vd.uniregctb.registrefoncier.TypeCommunaute;
import ch.vd.uniregctb.registrefoncier.key.AyantDroitRFKey;

public abstract class AyantDroitRFHelper {

	private AyantDroitRFHelper() {
	}

	public static boolean dataEquals(@NotNull AyantDroitRF left, @NotNull Rechteinhaber right) {
		if (right instanceof Personstamm) {
			return dataEquals(left, (Personstamm) right);
		}
		else if (right instanceof Gemeinschaft) {
			return dataEquals(left, (Gemeinschaft) right);
		}
		else {
			throw new IllegalArgumentException("Type d'ayant-droit inconnu = [" + right.getClass() + "]");
		}
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

	/**
	 * @return <b>vrai</b> si les deux ayant-droits spécifiés possèdent les mêmes données; <b>faux</b> autrement.
	 */
	public static boolean dataEquals(@NotNull AyantDroitRF left, @NotNull Gemeinschaft right) {
		if (!left.getIdRF().equals(right.getGemeinschatID())) {
			// erreur de programmation, on ne devrait jamais comparer deux ayant-droit avec des IDRef différents
			throw new ProgrammingException();
		}

		// [blindage] les valeurs suivantes ne doivent jamais changer (le modèle est construit sur ce prédicat)
		if (!(left instanceof CommunauteRF)) {
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		// [/blindage]

		final CommunauteRF communaute = (CommunauteRF) left;
		return communaute.getType() == getTypeCommunaute(right.getArt());
	}

	@Nullable
	private static TypeCommunaute getTypeCommunaute(@Nullable GemeinschaftsArt art) {
		if (art == null) {
			return null;
		}
		switch (art) {
		case EINFACHE_GESELLSCHAFT:
			return TypeCommunaute.SOCIETE_SIMPLE;
		case ERBENGEMEINSCHAFT:
			return TypeCommunaute.COMMUNAUTE_HEREDITAIRE;
		case GUETERGEMEINSCHAFT:
			return TypeCommunaute.COMMUNAUTE_DE_BIENS;
		case GEMEINDERSCHAFT:
			return TypeCommunaute.INDIVISION;
		case UNBEKANNT:
			return TypeCommunaute.INCONNU;
		default:
			throw new IllegalArgumentException("Type de communauté inconnu=[" + art + "]");
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

	public static AyantDroitRFKey newAyantDroitKey(@NotNull Rechteinhaber rechteinhaber) {
		if (rechteinhaber instanceof Personstamm) {
			return new AyantDroitRFKey(((Personstamm) rechteinhaber).getPersonstammID());
		}
		else if (rechteinhaber instanceof Gemeinschaft) {
			return new AyantDroitRFKey(((Gemeinschaft) rechteinhaber).getGemeinschatID());
		}
		else {
			throw new IllegalArgumentException("Type d'ayan-droit inconnu = [" + rechteinhaber.getClass() + "]");
		}
	}

	@NotNull
	public static AyantDroitRF newAyantDroitRF(@NotNull Rechteinhaber rechteinhaber) {

		final AyantDroitRF ayantDroitRF;
		if (rechteinhaber instanceof NatuerlichePersonstamm) {
			final NatuerlichePersonstamm natuerliche = (NatuerlichePersonstamm) rechteinhaber;
			PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF(natuerliche.getPersonstammID());
			pp.setNoRF(natuerliche.getNoRF());
			pp.setNoContribuable(getNoContribuable(natuerliche));
			pp.setPrenom(natuerliche.getVorname());
			pp.setNom(natuerliche.getName());
			pp.setDateNaissance(getRegDate(natuerliche.getGeburtsdatum()));
			ayantDroitRF = pp;
		}
		else if (rechteinhaber instanceof JuristischePersonstamm) {
			final JuristischePersonstamm juri = (JuristischePersonstamm) rechteinhaber;
			if (juri.getUnterart() == JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT) {
				final CollectivitePubliqueRF coll = new CollectivitePubliqueRF();
				coll.setIdRF(juri.getPersonstammID());
				coll.setNoRF(juri.getNoRF());
				coll.setNoContribuable(juri.getNrACI());
				coll.setRaisonSociale(juri.getName());
				ayantDroitRF = coll;
			}
			else {
				final PersonneMoraleRF pm = new PersonneMoraleRF();
				pm.setIdRF(juri.getPersonstammID());
				pm.setNoRF(juri.getNoRF());
				pm.setNoContribuable(juri.getNrACI());
				pm.setRaisonSociale(juri.getName());
				pm.setNumeroRC(juri.getFirmenNr());
				ayantDroitRF = pm;
			}
		}
		else if (rechteinhaber instanceof Gemeinschaft) {
			final Gemeinschaft gemeinschaft = (Gemeinschaft) rechteinhaber;
			CommunauteRF communaute = new CommunauteRF();
			communaute.setIdRF(gemeinschaft.getGemeinschatID());
			communaute.setType(getTypeCommunaute(gemeinschaft.getArt()));
			ayantDroitRF = communaute;
		}
		else {
			throw new IllegalArgumentException("Type d'ayant-droit inconnu = [" + rechteinhaber.getClass() + "]");
		}
		return ayantDroitRF;
	}

	private static RegDate getRegDate(@Nullable GeburtsDatum geburtsdatum) {
		if (geburtsdatum == null) {
			return null;
		}
		return RegDateHelper.get(geburtsdatum.getJahr(), geburtsdatum.getMonat(), geburtsdatum.getTag());
	}

	@Nullable
	static Long getNoContribuable(@NotNull Personstamm right) {
		Long no = trimToNull(right.getNrACI());
		if (no == null && right instanceof NatuerlichePersonstamm) {
			no = trimToNull(((NatuerlichePersonstamm) right).getNrIROLE());
		}
		return no;
	}

	/**
	 * @param number un nombre
	 * @return le nombre spécifié; ou <b>null</b> si le nombre vaut zéro.
	 */
	@Nullable
	private static Long trimToNull(@Nullable Long number) {
		if (number != null && number == 0L) {
			return null;
		}
		return number;
	}
}