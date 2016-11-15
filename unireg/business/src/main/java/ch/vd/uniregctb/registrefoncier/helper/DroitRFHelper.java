package ch.vd.uniregctb.registrefoncier.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.JuristischePersonGb;
import ch.vd.capitastra.grundstueck.NatuerlichePersonGb;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumsform;
import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.uniregctb.common.ProgrammingException;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;
import ch.vd.uniregctb.rf.GenrePropriete;

public class DroitRFHelper {

	public static DroitRFKey newDroitRFKey(@NotNull PersonEigentumAnteil droit) {
		return new DroitRFKey(droit.getMasterID());
	}

	public static boolean dataEquals(Set<DroitRF> droits, List<PersonEigentumAnteil> eigentums) {

		if ((droits == null || droits.isEmpty()) && (eigentums == null || eigentums.isEmpty())) {
			// les deux collections sont vides ou nulles
			return true;
		}
		else if (droits == null || eigentums == null) {
			// une seule collection est vide ou nulle
			return false;
		}
		else if (droits.size() != eigentums.size()) {
			// les collections ne sont pas de tailles identiques
			return false;
		}

		List<DroitRF> remaining = new ArrayList<>(droits);
		for (PersonEigentumAnteil e : eigentums) {
			boolean found = false;
			for (int i = 0; i < remaining.size(); i++) {
				DroitRF droitRF = remaining.get(i);
				if (droitRF.getMasterIdRF().equals(e.getMasterID()) &&  dataEquals(droitRF, e)) {
					remaining.remove(i);
					found = true;
					break;
				}
			}
			if (!found) {
				return false;
			}
		}

		// si on arrive là, c'est que les deux collections sont identiques
		return true;
	}

	public static boolean dataEquals(DroitRF droitRF, PersonEigentumAnteil personEigentumAnteil) {

		if (!droitRF.getMasterIdRF().equals(personEigentumAnteil.getMasterID())) {
			// erreur de programmation, on ne devrait jamais comparer deux droit avec des masterIDRef différents
			throw new ProgrammingException();
		}

		// [blindage] les valeurs suivantes ne doivent jamais changer (le modèle est construit sur ce prédicat)
		if (droitRF instanceof DroitProprietePersonnePhysiqueRF && personEigentumAnteil.getNatuerlichePersonGb() == null) {
			throw new IllegalArgumentException("Le type du droit masterIdRF=[" + droitRF.getMasterIdRF() + "] a changé.");
		}
		if (droitRF instanceof DroitProprietePersonneMoraleRF && personEigentumAnteil.getJuristischePersonGb() == null) {
			throw new IllegalArgumentException("Le type du droit masterIdRF=[" + droitRF.getMasterIdRF() + "] a changé.");
		}
		if (droitRF instanceof DroitProprieteCommunauteRF && personEigentumAnteil.getGemeinschaft() == null) {
			throw new IllegalArgumentException("Le type du droit masterIdRF=[" + droitRF.getMasterIdRF() + "] a changé.");
		}
		if (droitRF instanceof UsufruitRF || droitRF instanceof DroitHabitationRF) {
			// erreur de programmation, les usufruits et droits d'habitation ne sont pas définis avec le type ch.vd.capitastra.grundstueck.PersonEigentumAnteil
			throw new ProgrammingException();
		}
		// [/blindage]

		if (droitRF instanceof DroitProprietePersonnePhysiqueRF) {
			return dataEquals((DroitProprietePersonnePhysiqueRF) droitRF, personEigentumAnteil);
		}
		else if (droitRF instanceof DroitProprietePersonneMoraleRF) {
			return dataEquals((DroitProprietePersonneMoraleRF) droitRF, personEigentumAnteil);
		}
		else if (droitRF instanceof DroitProprieteCommunauteRF) {
			return dataEquals((DroitProprieteCommunauteRF) droitRF, personEigentumAnteil);
		}
		else {
			throw new IllegalArgumentException("Type de tiers RF inconnu=[" + droitRF.getClass() + "]");
		}
	}

	private static boolean dataEquals(DroitProprietePersonnePhysiqueRF droitRF, PersonEigentumAnteil personEigentumAnteil) {

		final NatuerlichePersonGb natuerlichePersonGb = personEigentumAnteil.getNatuerlichePersonGb();
		if (natuerlichePersonGb == null) {
			throw new ProgrammingException();
		}

		final Rechtsgrund rechtsgrund = getDroitDeReference(natuerlichePersonGb.getRechtsgruende());

		return communauteEquals(droitRF.getCommunaute(), natuerlichePersonGb.getGemeinschatIDREF()) &&
				ayantDroitEquals(droitRF.getAyantDroit(), natuerlichePersonGb.getPersonstammIDREF()) &&
				immeubleEquals(droitRF.getImmeuble(), personEigentumAnteil.getBelastetesGrundstueckIDREF()) &&
				numeroAffaireEquals(droitRF.getNumeroAffaire(), rechtsgrund) &&
				partEquals(droitRF.getPart(), personEigentumAnteil.getQuote()) &&
				regimeEquals(droitRF.getRegime(), personEigentumAnteil.getPersonEigentumsForm()) &&
				droitRF.getDateDebutOfficielle() == rechtsgrund.getBelegDatum() &&
				motifEquals(droitRF.getMotifDebut(), rechtsgrund.getRechtsgrundCode());
	}

	private static boolean dataEquals(DroitProprietePersonneMoraleRF droitRF, PersonEigentumAnteil personEigentumAnteil) {

		final JuristischePersonGb juristischePersonGb = personEigentumAnteil.getJuristischePersonGb();
		if (juristischePersonGb == null) {
			throw new ProgrammingException();
		}
		final Rechtsgrund rechtsgrund = getDroitDeReference(juristischePersonGb.getRechtsgruende());

		return communauteEquals(droitRF.getCommunaute(), juristischePersonGb.getGemeinschatIDREF()) &&
				ayantDroitEquals(droitRF.getAyantDroit(), juristischePersonGb.getPersonstammIDREF()) &&
				immeubleEquals(droitRF.getImmeuble(), personEigentumAnteil.getBelastetesGrundstueckIDREF()) &&
				numeroAffaireEquals(droitRF.getNumeroAffaire(), rechtsgrund) &&
				partEquals(droitRF.getPart(), personEigentumAnteil.getQuote()) &&
				regimeEquals(droitRF.getRegime(), personEigentumAnteil.getPersonEigentumsForm()) &&
				droitRF.getDateDebutOfficielle() == rechtsgrund.getBelegDatum() &&
				motifEquals(droitRF.getMotifDebut(), rechtsgrund.getRechtsgrundCode());
	}

	private static boolean dataEquals(DroitProprieteCommunauteRF droitRF, PersonEigentumAnteil personEigentumAnteil) {

		final Gemeinschaft gemeinschaft = personEigentumAnteil.getGemeinschaft();
		if (gemeinschaft == null) {
			throw new ProgrammingException();
		}

		final Rechtsgrund rechtsgrund = getDroitDeReference(gemeinschaft.getRechtsgruende());

		return ayantDroitEquals(droitRF.getAyantDroit(), gemeinschaft.getGemeinschatID()) &&
				immeubleEquals(droitRF.getImmeuble(), personEigentumAnteil.getBelastetesGrundstueckIDREF()) &&
				numeroAffaireEquals(droitRF.getNumeroAffaire(), rechtsgrund) &&
				partEquals(droitRF.getPart(), personEigentumAnteil.getQuote()) &&
				regimeEquals(droitRF.getRegime(), personEigentumAnteil.getPersonEigentumsForm()) &&
				droitRF.getDateDebutOfficielle() == rechtsgrund.getBelegDatum() &&
				motifEquals(droitRF.getMotifDebut(), rechtsgrund.getRechtsgrundCode());
	}

	public static GenrePropriete getRegime(@Nullable PersonEigentumsform form) {
		if (form == null) {
			return null;
		}
		switch (form) {
		case GESAMTEIGENTUM:
			return GenrePropriete.COMMUNE;
		case MITEIGENTUM:
			return GenrePropriete.COPROPRIETE;
		case ALLEINEIGENTUM:
			return GenrePropriete.INDIVIDUELLE;
		default:
			throw new IllegalArgumentException("Type de propriété inconnue = [" + form + "]");
		}
	}

	/**
	 * @return le droit de référence à utiliser, c'est-à-dire le droit le plus ancien (selon la spécification "Récupérer l'immeuble").
	 */
	@NotNull
	public static Rechtsgrund getDroitDeReference(@NotNull List<Rechtsgrund> rechtsgruende) {
		Rechtsgrund oldest = null;
		for (Rechtsgrund rechtsgrund : rechtsgruende) {
			if (oldest == null || rechtsgrund.getBelegDatum().isBefore(oldest.getBelegDatum())) {
				oldest = rechtsgrund;
			}
		}
		if (oldest == null) {
			throw new IllegalArgumentException("Il n'y a pas de droit sur le lien");
		}
		return oldest;
	}

	private static boolean motifEquals(@NotNull String motif, @NotNull CapiCode rechtsgrundCode) {
		return Objects.equals(motif, rechtsgrundCode.getTextFr());
	}

	private static boolean regimeEquals(@NotNull GenrePropriete regime, @NotNull PersonEigentumsform personEigentumsForm) {
		return regime == getRegime(personEigentumsForm);
	}

	private static boolean partEquals(@Nullable Fraction part, @Nullable Quote quote) {
		if (part == null || quote == null) {
			return part == null && quote == null;
		}
		else {
			return part.getNumerateur() == quote.getAnteilZaehler().intValue() &&
					part.getDenominateur() == quote.getAnteilNenner().intValue();
		}
	}

	private static boolean numeroAffaireEquals(@NotNull IdentifiantAffaireRF numeroAffaire, @NotNull Rechtsgrund rechtsgrund) {
		return numeroAffaire.getNumeroOffice() == rechtsgrund.getAmtNummer() &&
				numeroAffaire.getAnnee() == rechtsgrund.getBelegJahr() &&
				numeroAffaire.getNumero() == rechtsgrund.getBelegNummer() &&
				numeroAffaire.getIndex() == rechtsgrund.getBelegNummerIndex();
	}

	private static boolean immeubleEquals(@NotNull ImmeubleRF immeuble, @NotNull String grundstueckIDREF) {
		return immeuble.getIdRF().equals(grundstueckIDREF);
	}

	private static boolean communauteEquals(@Nullable CommunauteRF communaute, @Nullable String gemeinschatIDREF) {
		final String idRF = communaute == null ? null : communaute.getIdRF();
		return Objects.equals(idRF, gemeinschatIDREF);
	}

	private static boolean ayantDroitEquals(@NotNull AyantDroitRF ayantDroit, @NotNull String personstammIDREF) {
		return ayantDroit.getIdRF().equals(personstammIDREF);
	}

	public static String getIdRF(@NotNull PersonEigentumAnteil eigentumAnteil) {

		final Gemeinschaft gemeinschaft = eigentumAnteil.getGemeinschaft();
		final NatuerlichePersonGb natuerlichePerson = eigentumAnteil.getNatuerlichePersonGb();
		final JuristischePersonGb juristischePerson = eigentumAnteil.getJuristischePersonGb();

		if (gemeinschaft != null) {
			return gemeinschaft.getGemeinschatID();
		}
		else if (natuerlichePerson != null) {
			return natuerlichePerson.getPersonstammIDREF();
		}
		else if (juristischePerson != null) {
			return juristischePerson.getPersonstammIDREF();
		}
		else {
			throw new IllegalArgumentException("Type de droit inconnu masterIdRF=[" + eigentumAnteil.getMasterID()+"]");
		}
	}
}
