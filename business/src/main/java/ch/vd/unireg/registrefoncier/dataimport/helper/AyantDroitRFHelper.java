package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.common.Rechteinhaber;
import ch.vd.capitastra.grundstueck.GeburtsDatum;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GemeinschaftsArt;
import ch.vd.capitastra.grundstueck.Grundstueck;
import ch.vd.capitastra.grundstueck.JuristischePersonUnterart;
import ch.vd.capitastra.grundstueck.JuristischePersonstamm;
import ch.vd.capitastra.grundstueck.NatuerlichePersonstamm;
import ch.vd.capitastra.grundstueck.Personstamm;
import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.unireg.common.ProgrammingException;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CollectivitePubliqueRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.ImmeubleBeneficiaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.PersonneMoraleRF;
import ch.vd.unireg.registrefoncier.PersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.TypeCommunaute;
import ch.vd.unireg.registrefoncier.key.AyantDroitRFKey;

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
		else if (right instanceof ch.vd.capitastra.rechteregister.Personstamm) {
			return dataEquals(left, (ch.vd.capitastra.rechteregister.Personstamm) right);
		}
		else if (right instanceof Grundstueck) {
			return dataEquals(left, (Grundstueck) right);
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
	public static boolean dataEquals(@NotNull AyantDroitRF left, @NotNull ch.vd.capitastra.rechteregister.Personstamm right) {
		if (!left.getIdRF().equals(right.getPersonstammID())) {
			// erreur de programmation, on ne devrait jamais comparer deux ayant-droit avec des IDRef différents
			throw new ProgrammingException();
		}

		// [blindage] les valeurs suivantes ne doivent jamais changer (le modèle est construit sur ce prédicat)
		if (left instanceof PersonnePhysiqueRF && !(right instanceof ch.vd.capitastra.rechteregister.NatuerlichePersonstamm)) {
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		if (left instanceof PersonneMoraleRF && (!(right instanceof ch.vd.capitastra.rechteregister.JuristischePersonstamm) ||
				((ch.vd.capitastra.rechteregister.JuristischePersonstamm) right).getUnterart() == ch.vd.capitastra.rechteregister.JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT)) {
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		if (left instanceof CollectivitePubliqueRF && (!(right instanceof ch.vd.capitastra.rechteregister.JuristischePersonstamm) ||
				((ch.vd.capitastra.rechteregister.JuristischePersonstamm) right).getUnterart() != ch.vd.capitastra.rechteregister.JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT)) {
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		if (left instanceof CommunauteRF) {
			// les communautés RF sont renseignées à partir des droits, on ne devrait pas en recevoir par ce canal
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		// [/blindage]

		if (left instanceof PersonnePhysiqueRF) {
			return dataEquals((PersonnePhysiqueRF) left, (ch.vd.capitastra.rechteregister.NatuerlichePersonstamm) right);
		}
		else if (left instanceof PersonneMoraleRF) {
			return dataEquals((PersonneMoraleRF) left, (ch.vd.capitastra.rechteregister.JuristischePersonstamm) right);
		}
		else if (left instanceof CollectivitePubliqueRF) {
			return dataEquals((CollectivitePubliqueRF) left, (ch.vd.capitastra.rechteregister.JuristischePersonstamm) right);
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

	/**
	 * @return <b>vrai</b> si les deux ayant-droits spécifiés possèdent les mêmes données; <b>faux</b> autrement.
	 */
	public static boolean dataEquals(@NotNull AyantDroitRF left, @NotNull Grundstueck right) {
		// [blindage] les valeurs suivantes ne doivent jamais changer (le modèle est construit sur ce prédicat)
		if (!(left instanceof ImmeubleBeneficiaireRF)) {
			throw new IllegalArgumentException("Le type de l'ayant-droit idRF=[" + left.getIdRF() + "] a changé.");
		}
		// [/blindage]

		// la seule valeur mémorisée sur un immeuble bénéficiaire est l'IdRF de l'immeuble correspondant.
		return Objects.equals(left.getIdRF(), right.getGrundstueckID());
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

	public static boolean dataEquals(@NotNull PersonnePhysiqueRF left, @NotNull ch.vd.capitastra.rechteregister.NatuerlichePersonstamm right) {
		return // le numéro RF n'existe pas dans le rechtregister : left.getNoRF() == right.getNoRF() &&
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

	public static boolean dataEquals(@NotNull PersonneMoraleRF left, @NotNull ch.vd.capitastra.rechteregister.JuristischePersonstamm right) {
		return // le numéro RF n'existe pas dans le rechtregister : left.getNoRF() == right.getNoRF() &&
				(Objects.equals(left.getNoContribuable(), getNoContribuable(right))) &&
				Objects.equals(left.getRaisonSociale(), right.getName());
	}

	public static boolean dataEquals(@NotNull CollectivitePubliqueRF left, @NotNull JuristischePersonstamm right) {
		return left.getNoRF() == right.getNoRF() &&
				(Objects.equals(left.getNoContribuable(), getNoContribuable(right))) &&
				Objects.equals(left.getRaisonSociale(), right.getName());
	}

	public static boolean dataEquals(@NotNull CollectivitePubliqueRF left, @NotNull ch.vd.capitastra.rechteregister.JuristischePersonstamm right) {
		return // le numéro RF n'existe pas dans le rechtregister : left.getNoRF() == right.getNoRF() &&
				(Objects.equals(left.getNoContribuable(), getNoContribuable(right))) &&
				Objects.equals(left.getRaisonSociale(), right.getName());
	}

	public static boolean idRFEquals(@NotNull AyantDroitRF left, @NotNull AyantDroitRF right) {
		return Objects.equals(left.getIdRF(), right.getIdRF());
	}

	public static AyantDroitRFKey newAyantDroitKey(@NotNull Rechteinhaber rechteinhaber) {
		if (rechteinhaber instanceof Personstamm) {
			return new AyantDroitRFKey(((Personstamm) rechteinhaber).getPersonstammID());
		}
		else if (rechteinhaber instanceof Gemeinschaft) {
			return new AyantDroitRFKey(((Gemeinschaft) rechteinhaber).getGemeinschatID());
		}
		else if (rechteinhaber instanceof ch.vd.capitastra.rechteregister.Personstamm) {
			return new AyantDroitRFKey(((ch.vd.capitastra.rechteregister.Personstamm) rechteinhaber).getPersonstammID());
		}
		else if (rechteinhaber instanceof Grundstueck) {
			return new AyantDroitRFKey(((Grundstueck) rechteinhaber).getGrundstueckID());
		}
		else {
			throw new IllegalArgumentException("Type d'ayant-droit inconnu = [" + rechteinhaber.getClass() + "]");
		}
	}

	@NotNull
	public static AyantDroitRF newAyantDroitRF(@NotNull Rechteinhaber rechteinhaber, @NotNull Function<String, ImmeubleRF> immeubleProvider) {

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
		else if (rechteinhaber instanceof ch.vd.capitastra.rechteregister.NatuerlichePersonstamm) {
			final ch.vd.capitastra.rechteregister.NatuerlichePersonstamm natuerliche = (ch.vd.capitastra.rechteregister.NatuerlichePersonstamm) rechteinhaber;
			PersonnePhysiqueRF pp = new PersonnePhysiqueRF();
			pp.setIdRF(natuerliche.getPersonstammID());
			// le numéro RF n'existe pas dans le rechtregister : pp.setNoRF(natuerliche.getNoRF());
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
				coll.setNoContribuable(getNoContribuable(juri));
				coll.setRaisonSociale(juri.getName());
				ayantDroitRF = coll;
			}
			else {
				final PersonneMoraleRF pm = new PersonneMoraleRF();
				pm.setIdRF(juri.getPersonstammID());
				pm.setNoRF(juri.getNoRF());
				pm.setNoContribuable(getNoContribuable(juri));
				pm.setRaisonSociale(juri.getName());
				pm.setNumeroRC(juri.getFirmenNr());
				ayantDroitRF = pm;
			}
		}
		else if (rechteinhaber instanceof ch.vd.capitastra.rechteregister.JuristischePersonstamm) {
			final ch.vd.capitastra.rechteregister.JuristischePersonstamm juri = (ch.vd.capitastra.rechteregister.JuristischePersonstamm) rechteinhaber;
			if (juri.getUnterart() == ch.vd.capitastra.rechteregister.JuristischePersonUnterart.OEFFENTLICHE_KOERPERSCHAFT) {
				final CollectivitePubliqueRF coll = new CollectivitePubliqueRF();
				coll.setIdRF(juri.getPersonstammID());
				// le numéro RF n'existe pas dans le rechtregister : coll.setNoRF(juri.getNoRF());
				coll.setNoContribuable(getNoContribuable(juri));
				coll.setRaisonSociale(juri.getName());
				ayantDroitRF = coll;
			}
			else {
				final PersonneMoraleRF pm = new PersonneMoraleRF();
				pm.setIdRF(juri.getPersonstammID());
				// le numéro RF n'existe pas dans le rechtregister : pm.setNoRF(juri.getNoRF());
				pm.setNoContribuable(getNoContribuable(juri));
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
		else if (rechteinhaber instanceof Grundstueck) {
			final Grundstueck grundstueck =(Grundstueck) rechteinhaber;
			final String idRF = grundstueck.getGrundstueckID();
			final ImmeubleRF immeuble = immeubleProvider.apply(idRF);
			final ImmeubleBeneficiaireRF dominant = new ImmeubleBeneficiaireRF();
			dominant.setIdRF(idRF);
			dominant.setImmeuble(immeuble);
			ayantDroitRF = dominant;
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

	private static RegDate getRegDate(@Nullable ch.vd.capitastra.rechteregister.GeburtsDatum geburtsdatum) {
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

	@Nullable
	static Long getNoContribuable(@NotNull ch.vd.capitastra.rechteregister.Personstamm right) {
		Long no = trimToNull(right.getNrACI());
		if (no == null && right instanceof ch.vd.capitastra.rechteregister.NatuerlichePersonstamm) {
			no = trimToNull(((ch.vd.capitastra.rechteregister.NatuerlichePersonstamm) right).getNrIROLE());
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