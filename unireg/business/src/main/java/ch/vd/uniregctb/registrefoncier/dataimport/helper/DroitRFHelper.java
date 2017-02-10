package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.CapiCode;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.JuristischePersonGb;
import ch.vd.capitastra.grundstueck.NatuerlichePersonGb;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumsform;
import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDateHelper;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;
import ch.vd.uniregctb.rf.GenrePropriete;

public class DroitRFHelper {

	public static DroitRFKey newDroitRFKey(@NotNull PersonEigentumAnteil droit) {
		return new DroitRFKey(droit.getMasterID());
	}

	public static boolean dataEquals(Set<DroitRF> droits, List<PersonEigentumAnteil> eigentums, boolean importInitial) {

		//noinspection Duplicates
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
				if (dataEquals(droitRF, e, importInitial)) {
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

	public static boolean dataEquals(DroitRF droitRF, PersonEigentumAnteil personEigentumAnteil, boolean importInitial) {
		return dataEquals(droitRF, get(personEigentumAnteil, importInitial, DroitRFHelper::simplisticAyantDroitProvider, DroitRFHelper::simplisticCommunauteProvider, DroitRFHelper::simplisticImmeubleProvider));
	}

	/**
	 * Provider d'ayant-droit simplifié au maximum pour retourner un ayant-droit avec juste l'idRF de renseigné.
	 */
	@NotNull
	private static AyantDroitRF simplisticAyantDroitProvider(String idRef) {
		final AyantDroitRF i = new AyantDroitRF() {
		};
		i.setIdRF(idRef);
		return i;
	}

	/**
	 * Provider d'immeuble simplifié au maximum pour retourner un immeuble avec juste l'idRF de renseigné.
	 */
	@NotNull
	private static ImmeubleRF simplisticImmeubleProvider(String idRef) {
		final ImmeubleRF i = new ImmeubleRF() {
		};
		i.setIdRF(idRef);
		return i;
	}

	/**
	 * Provider de communauté simplifié au maximum pour retourner une communauté avec juste l'idRF de renseigné.
	 */
	@Nullable
	private static CommunauteRF simplisticCommunauteProvider(@Nullable String idRf) {
		if (idRf == null) {
			return null;
		}
		final CommunauteRF c = new CommunauteRF();
		c.setIdRF(idRf);
		return c;
	}

	public static boolean dataEquals(@NotNull DroitRF left, @NotNull DroitRF right) {

		if (!left.getMasterIdRF().equals(right.getMasterIdRF())) {
			return false;
		}

		if (!left.getClass().equals(right.getClass())) {
			return false;
		}

		if (left instanceof DroitProprietePersonnePhysiqueRF) {
			return equalsDroitPropPP((DroitProprietePersonnePhysiqueRF) left, (DroitProprietePersonnePhysiqueRF) right);
		}
		else if (left instanceof DroitProprietePersonneMoraleRF) {
			return equalsDroitPropPM((DroitProprietePersonneMoraleRF) left, (DroitProprietePersonneMoraleRF) right);
		}
		else if (left instanceof DroitProprieteCommunauteRF) {
			return equalsDroitProp((DroitProprieteCommunauteRF) left, (DroitProprieteCommunauteRF) right);
		}
		else {
			throw new IllegalArgumentException("Type de tiers RF inconnu=[" + left.getClass() + "]");
		}
	}

	private static boolean equalsDroitPropPP(@NotNull DroitProprietePersonnePhysiqueRF left, @NotNull DroitProprietePersonnePhysiqueRF right) {
		return communauteEquals(left.getCommunaute(), right.getCommunaute()) &&
				equalsDroitProp(left, right);
	}

	private static boolean equalsDroitPropPM(@NotNull DroitProprietePersonneMoraleRF left, @NotNull DroitProprietePersonneMoraleRF right) {
		return communauteEquals(left.getCommunaute(), right.getCommunaute()) &&
				equalsDroitProp(left, right);
	}

	private static boolean equalsDroitProp(@NotNull DroitProprieteRF left, @NotNull DroitProprieteRF right) {
		return ayantDroitEquals(left.getAyantDroit(), right.getAyantDroit()) &&
				immeubleEquals(left.getImmeuble(), right.getImmeuble()) &&
				numeroAffaireEquals(left.getNumeroAffaire(), right.getNumeroAffaire()) &&
				partEquals(left.getPart(), right.getPart()) &&
				left.getRegime() == right.getRegime() &&
				left.getDateDebutOfficielle() == right.getDateDebutOfficielle() &&
				Objects.equals(left.getMotifDebut(), right.getMotifDebut());
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
	 * @return le droit de référence à utiliser, c'est-à-dire (selon la spécification "Récupérer l'immeuble") :
	 * <ul>
	 *     <li>le droit le plus ancien, dans le cas de l'import initial (SIFISC-22400).</li>
	 *     <li>le droit le plus récent, dans tous les autres cas.</li>
	 * </ul>
	 */
	@Nullable
	public static Rechtsgrund getDroitDeReference(@Nullable List<Rechtsgrund> rechtsgruende, boolean importInitial) {
		if (rechtsgruende == null || rechtsgruende.isEmpty()) {
			return null;
		}
		Rechtsgrund oldest = null;
		Rechtsgrund newest = null;
		for (Rechtsgrund rechtsgrund : rechtsgruende) {
			if (oldest == null || RegDateHelper.isBefore(rechtsgrund.getBelegDatum(), oldest.getBelegDatum(), NullDateBehavior.EARLIEST)) {
				oldest = rechtsgrund;
			}
			if (newest == null || RegDateHelper.isAfter(rechtsgrund.getBelegDatum(), newest.getBelegDatum(), NullDateBehavior.EARLIEST)) {
				newest = rechtsgrund;
			}
		}
		return importInitial ? oldest : newest;
	}

	private static boolean partEquals(@Nullable Fraction part, @Nullable Fraction quote) {
		if (part == null || quote == null) {
			return part == null && quote == null;
		}
		else {
			return part.getNumerateur() == quote.getNumerateur() &&
					part.getDenominateur() == quote.getDenominateur();
		}
	}

	private static boolean numeroAffaireEquals(@Nullable IdentifiantAffaireRF left, @Nullable IdentifiantAffaireRF right) {
		if (left == null || right == null) {
			return left == null && right == null;
		}
		else {
			return left.getNumeroOffice() == right.getNumeroOffice() &&
					Objects.equals(left.getNumeroAffaire(), right.getNumeroAffaire());
		}
	}

	private static boolean immeubleEquals(@NotNull ImmeubleRF left, @NotNull ImmeubleRF right) {
		return Objects.equals(left.getIdRF(), right.getIdRF());
	}

	private static boolean communauteEquals(@Nullable CommunauteRF left, @Nullable CommunauteRF right) {
		if (left == null || right == null) {
			return left == null && right == null;
		}
		else {
			return Objects.equals(left.getIdRF(), right.getIdRF());
		}
	}

	private static boolean ayantDroitEquals(@NotNull AyantDroitRF left, @NotNull AyantDroitRF right) {
		return Objects.equals(left.getIdRF(), right.getIdRF());
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
			throw new IllegalArgumentException("Type de droit inconnu masterIdRF=[" + eigentumAnteil.getMasterID() + "]");
		}
	}

	@NotNull
	public static DroitProprieteRF newDroitRF(@NotNull PersonEigentumAnteil eigentumAnteil,
	                                          boolean importInitial, @NotNull Function<String, AyantDroitRF> ayantDroitProvider,
	                                          @NotNull Function<String, CommunauteRF> communauteProvider,
	                                          @NotNull Function<String, ImmeubleRF> immeubleProvider) {

		final NatuerlichePersonGb natuerlichePerson = eigentumAnteil.getNatuerlichePersonGb();
		final JuristischePersonGb juristischePerson = eigentumAnteil.getJuristischePersonGb();
		final Gemeinschaft gemeinschaft = eigentumAnteil.getGemeinschaft();

		final DroitProprieteRF droit;
		if (natuerlichePerson != null) {

			final DroitProprietePersonnePhysiqueRF d = new DroitProprietePersonnePhysiqueRF();
			final Rechtsgrund rechtsgrund = getDroitDeReference(natuerlichePerson.getRechtsgruende(), importInitial);

			d.setMasterIdRF(eigentumAnteil.getMasterID());
			d.setAyantDroit(ayantDroitProvider.apply(natuerlichePerson.getPersonstammIDREF()));
			d.setCommunaute(communauteProvider.apply(natuerlichePerson.getGemeinschatIDREF()));
			d.setImmeuble(immeubleProvider.apply(eigentumAnteil.getBelastetesGrundstueckIDREF()));
			d.setNumeroAffaire(getAffaire(rechtsgrund));
			d.setPart(FractionHelper.get(eigentumAnteil.getQuote()));
			d.setRegime(getRegime(eigentumAnteil.getPersonEigentumsForm()));
			if (rechtsgrund != null) {
				d.setDateDebutOfficielle(rechtsgrund.getBelegDatum());
				d.setMotifDebut(getMotif(rechtsgrund.getRechtsgrundCode()));
			}

			droit = d;
		}
		else if (juristischePerson != null) {

			final DroitProprietePersonneMoraleRF d = new DroitProprietePersonneMoraleRF();
			final Rechtsgrund rechtsgrund = getDroitDeReference(juristischePerson.getRechtsgruende(), importInitial);

			d.setMasterIdRF(eigentumAnteil.getMasterID());
			d.setAyantDroit(ayantDroitProvider.apply(juristischePerson.getPersonstammIDREF()));
			d.setCommunaute(communauteProvider.apply(juristischePerson.getGemeinschatIDREF()));
			d.setImmeuble(immeubleProvider.apply(eigentumAnteil.getBelastetesGrundstueckIDREF()));
			d.setNumeroAffaire(getAffaire(rechtsgrund));
			d.setPart(FractionHelper.get(eigentumAnteil.getQuote()));
			d.setRegime(getRegime(eigentumAnteil.getPersonEigentumsForm()));
			if (rechtsgrund != null) {
				d.setDateDebutOfficielle(rechtsgrund.getBelegDatum());
				d.setMotifDebut(getMotif(rechtsgrund.getRechtsgrundCode()));
			}

			droit = d;
		}
		else if (gemeinschaft != null) {

			final DroitProprieteCommunauteRF d = new DroitProprieteCommunauteRF();
			final Rechtsgrund rechtsgrund = getDroitDeReference(gemeinschaft.getRechtsgruende(), importInitial);

			d.setMasterIdRF(eigentumAnteil.getMasterID());
			d.setAyantDroit(ayantDroitProvider.apply(gemeinschaft.getGemeinschatID()));
			d.setImmeuble(immeubleProvider.apply(eigentumAnteil.getBelastetesGrundstueckIDREF()));
			d.setNumeroAffaire(getAffaire(rechtsgrund));
			d.setPart(FractionHelper.get(eigentumAnteil.getQuote()));
			d.setRegime(getRegime(eigentumAnteil.getPersonEigentumsForm()));
			if (rechtsgrund != null) {
				d.setDateDebutOfficielle(rechtsgrund.getBelegDatum());
				d.setMotifDebut(getMotif(rechtsgrund.getRechtsgrundCode()));
			}
			droit = d;
		}
		else {
			throw new IllegalArgumentException("Type de droit inconnu masterIdRf=[" + eigentumAnteil.getMasterID() + "]");
		}

		return droit;
	}

	@Nullable
	private static String getMotif(@Nullable CapiCode code) {
		if (code == null) {
			return null;
		}
		return code.getTextFr();
	}

	@Nullable
	static IdentifiantAffaireRF getAffaire(@Nullable Rechtsgrund rechtsgrund) {
		if (rechtsgrund == null) {
			return null;
		}
		final String belegAlt = rechtsgrund.getBelegAlt();
		if (StringUtils.isNotBlank(belegAlt)) {
			return new IdentifiantAffaireRF(rechtsgrund.getAmtNummer(), belegAlt);
		}
		else {
			return new IdentifiantAffaireRF(rechtsgrund.getAmtNummer(), rechtsgrund.getBelegJahr(), rechtsgrund.getBelegNummer(), rechtsgrund.getBelegNummerIndex());
		}
	}

	@Nullable
	public static DroitProprieteRF get(@Nullable PersonEigentumAnteil eigentumAnteil,
	                                   boolean importInitial, @NotNull Function<String, AyantDroitRF> ayantDroitProvider,
	                                   @NotNull Function<String, CommunauteRF> communauteProvider,
	                                   @NotNull Function<String, ImmeubleRF> immeubleProvider) {
		if (eigentumAnteil == null) {
			return null;
		}
		return newDroitRF(eigentumAnteil, importInitial, ayantDroitProvider, communauteProvider, immeubleProvider);
	}
}
