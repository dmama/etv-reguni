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
import ch.vd.capitastra.grundstueck.EigentumAnteil;
import ch.vd.capitastra.grundstueck.Gemeinschaft;
import ch.vd.capitastra.grundstueck.GrundstueckEigentumAnteil;
import ch.vd.capitastra.grundstueck.GrundstueckEigentumsform;
import ch.vd.capitastra.grundstueck.JuristischePersonGb;
import ch.vd.capitastra.grundstueck.NatuerlichePersonGb;
import ch.vd.capitastra.grundstueck.PersonEigentumAnteil;
import ch.vd.capitastra.grundstueck.PersonEigentumsform;
import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.CommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.uniregctb.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.uniregctb.registrefoncier.DroitProprieteRF;
import ch.vd.uniregctb.registrefoncier.DroitRF;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.rf.GenrePropriete;

public abstract class DroitRFHelper {

	private DroitRFHelper() {
	}

	public static boolean dataEquals(Set<DroitProprieteRF> droits, List<EigentumAnteil> eigentums) {

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

		List<DroitProprieteRF> remaining = new ArrayList<>(droits);
		for (EigentumAnteil e : eigentums) {
			boolean found = false;
			for (int i = 0; i < remaining.size(); i++) {
				DroitProprieteRF droitRF = remaining.get(i);
				if (dataEquals(droitRF, e)) {
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

	public static boolean dataEquals(DroitProprieteRF droitRF, EigentumAnteil eigentumAnteil) {
		return dataEquals(droitRF, get(eigentumAnteil, DroitRFHelper::simplisticAyantDroitProvider, DroitRFHelper::simplisticCommunauteProvider, DroitRFHelper::simplisticImmeubleProvider));
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

	public static boolean masterIdAndVersionIdEquals(@NotNull DroitRF left, @NotNull DroitRF right) {
		return Objects.equals(left.getMasterIdRF(), right.getMasterIdRF()) &&
				Objects.equals(left.getVersionIdRF(), right.getVersionIdRF());
	}

	public static boolean dataEquals(@NotNull DroitProprieteRF left, @NotNull DroitProprieteRF right) {

		if (!masterIdAndVersionIdEquals(left, right)) {
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
			return equalsDroitPropComm((DroitProprieteCommunauteRF) left, (DroitProprieteCommunauteRF) right);
		}
		else if (left instanceof DroitProprieteImmeubleRF) {
			return equalsDroitPropImm((DroitProprieteImmeubleRF) left, (DroitProprieteImmeubleRF) right);
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

	private static boolean equalsDroitPropComm(@NotNull DroitProprieteCommunauteRF left, @NotNull DroitProprieteCommunauteRF right) {
		return equalsDroitProp(left, right);
	}

	private static boolean equalsDroitPropImm(@NotNull DroitProprieteImmeubleRF left, @NotNull DroitProprieteImmeubleRF right) {
		return equalsDroitProp(left, right);
	}

	private static boolean equalsDroitProp(@NotNull DroitProprieteRF left, @NotNull DroitProprieteRF right) {
		return ayantDroitEquals(left.getAyantDroit(), right.getAyantDroit()) &&
				immeubleEquals(left.getImmeuble(), right.getImmeuble()) &&
				partEquals(left.getPart(), right.getPart()) &&
				left.getRegime() == right.getRegime() &&
				RaisonAcquisitionRFHelper.dataEquals(left.getRaisonsAcquisition(), right.getRaisonsAcquisition()) &&
				equalsDroit(left, right);
	}

	public static boolean equalsDroit(@NotNull DroitRF left, @NotNull DroitRF right) {
		return left.getDateDebutMetier() == right.getDateDebutMetier() &&
				// la date de fin métier n'est pas renseignée dans le fichier d'entrée, on ne la compare pas
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
			throw new IllegalArgumentException("Régime de propriété inconnu = [" + form + "]");
		}
	}

	private static GenrePropriete getRegime(@Nullable GrundstueckEigentumsform form) {
		if (form == null) {
			return null;
		}
		switch (form) {
		case MITEIGENTUM:
			return GenrePropriete.COPROPRIETE;
		case STOCKWERK:
			return GenrePropriete.PPE;
		case DOMINIERENDES_GRUNDSTUECK:
			return GenrePropriete.FONDS_DOMINANT;
		default:
			throw new IllegalArgumentException("Régime de propriété inconnu = [" + form + "]");
		}
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

	public static boolean numeroAffaireEquals(@Nullable IdentifiantAffaireRF left, @Nullable IdentifiantAffaireRF right) {
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

	public static boolean communauteEquals(@Nullable CommunauteRF left, @Nullable CommunauteRF right) {
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

	public static String getAyantDroitIdRF(@NotNull EigentumAnteil eigentumAnteil) {

		if (eigentumAnteil instanceof PersonEigentumAnteil) {
			final PersonEigentumAnteil pea =(PersonEigentumAnteil) eigentumAnteil;
			final Gemeinschaft gemeinschaft = pea.getGemeinschaft();
			final NatuerlichePersonGb natuerlichePerson = pea.getNatuerlichePersonGb();
			final JuristischePersonGb juristischePerson = pea.getJuristischePersonGb();

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
		else if (eigentumAnteil instanceof GrundstueckEigentumAnteil) {
			final GrundstueckEigentumAnteil gea =(GrundstueckEigentumAnteil) eigentumAnteil;
			// l'IDRef du fonds dominant est utilisé comme IDRef de l'ayant-droit.
			return gea.getBerechtigtesGrundstueckIDREF();
		}
		else {
			throw new IllegalArgumentException("Type de droit inconnu = [" + eigentumAnteil.getClass().getSimpleName() + "]");
		}
	}

	@NotNull
	public static DroitProprieteRF newDroitRF(@NotNull EigentumAnteil eigentumAnteil,
	                                          @NotNull Function<String, AyantDroitRF> ayantDroitProvider,
	                                          @NotNull Function<String, CommunauteRF> communauteProvider,
	                                          @NotNull Function<String, ImmeubleRF> immeubleProvider) {

		final DroitProprieteRF droit;
		if (eigentumAnteil instanceof PersonEigentumAnteil) {
			final PersonEigentumAnteil pea =(PersonEigentumAnteil) eigentumAnteil;

			final NatuerlichePersonGb natuerlichePerson = pea.getNatuerlichePersonGb();
			final JuristischePersonGb juristischePerson = pea.getJuristischePersonGb();
			final Gemeinschaft gemeinschaft = pea.getGemeinschaft();

			if (natuerlichePerson != null) {

				final DroitProprietePersonnePhysiqueRF d = new DroitProprietePersonnePhysiqueRF();
				d.setMasterIdRF(pea.getMasterID());
				d.setVersionIdRF(pea.getVersionID());
				d.setAyantDroit(ayantDroitProvider.apply(natuerlichePerson.getPersonstammIDREF()));
				d.setCommunaute(communauteProvider.apply(natuerlichePerson.getGemeinschatIDREF()));
				d.setImmeuble(immeubleProvider.apply(pea.getBelastetesGrundstueckIDREF()));
				d.setPart(FractionHelper.get(pea.getQuote()));
				d.setRegime(getRegime(pea.getPersonEigentumsForm()));
				natuerlichePerson.getRechtsgruende().forEach(r -> d.addRaisonAcquisition(RaisonAcquisitionRFHelper.newRaisonAcquisition(r)));

				droit = d;
			}
			else if (juristischePerson != null) {

				final DroitProprietePersonneMoraleRF d = new DroitProprietePersonneMoraleRF();
				d.setMasterIdRF(pea.getMasterID());
				d.setVersionIdRF(pea.getVersionID());
				d.setAyantDroit(ayantDroitProvider.apply(juristischePerson.getPersonstammIDREF()));
				d.setCommunaute(communauteProvider.apply(juristischePerson.getGemeinschatIDREF()));
				d.setImmeuble(immeubleProvider.apply(pea.getBelastetesGrundstueckIDREF()));
				d.setPart(FractionHelper.get(pea.getQuote()));
				d.setRegime(getRegime(pea.getPersonEigentumsForm()));
				juristischePerson.getRechtsgruende().forEach(r -> d.addRaisonAcquisition(RaisonAcquisitionRFHelper.newRaisonAcquisition(r)));

				droit = d;
			}
			else if (gemeinschaft != null) {

				final DroitProprieteCommunauteRF d = new DroitProprieteCommunauteRF();
				d.setMasterIdRF(pea.getMasterID());
				d.setVersionIdRF(pea.getVersionID());
				d.setAyantDroit(ayantDroitProvider.apply(gemeinschaft.getGemeinschatID()));
				d.setImmeuble(immeubleProvider.apply(pea.getBelastetesGrundstueckIDREF()));
				d.setPart(FractionHelper.get(pea.getQuote()));
				d.setRegime(getRegime(pea.getPersonEigentumsForm()));
				gemeinschaft.getRechtsgruende().forEach(r -> d.addRaisonAcquisition(RaisonAcquisitionRFHelper.newRaisonAcquisition(r)));

				droit = d;
			}
			else {
				throw new IllegalArgumentException("Type de droit inconnu masterIdRf=[" + pea.getMasterID() + "]");
			}
		}
		else if (eigentumAnteil instanceof GrundstueckEigentumAnteil) {
			final GrundstueckEigentumAnteil gea =(GrundstueckEigentumAnteil) eigentumAnteil;

			final DroitProprieteImmeubleRF d = new DroitProprieteImmeubleRF();
			d.setMasterIdRF(gea.getMasterID());
			d.setVersionIdRF(gea.getVersionID());
			d.setAyantDroit(ayantDroitProvider.apply(gea.getBerechtigtesGrundstueckIDREF()));   // l'IDRef de l'immeuble est réutilisé pour le ImmeubleBeneficiaireRF.
			d.setImmeuble(immeubleProvider.apply(gea.getBelastetesGrundstueckIDREF()));
			d.setPart(FractionHelper.get(gea.getQuote()));
			d.setRegime(getRegime(gea.getGrundstueckEigentumsForm()));
			gea.getRechtsgruende().forEach(r -> d.addRaisonAcquisition(RaisonAcquisitionRFHelper.newRaisonAcquisition(r)));

			droit = d;
		}
		else {
			throw new IllegalArgumentException("Type de droit inconnu = [" + eigentumAnteil.getClass().getSimpleName() + "]");
		}

		droit.calculateDateEtMotifDebut();
		return droit;
	}

	@Nullable
	public static String getMotif(@Nullable CapiCode code) {
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
	public static DroitProprieteRF get(@Nullable EigentumAnteil eigentumAnteil,
	                                   @NotNull Function<String, AyantDroitRF> ayantDroitProvider,
	                                   @NotNull Function<String, CommunauteRF> communauteProvider,
	                                   @NotNull Function<String, ImmeubleRF> immeubleProvider) {
		if (eigentumAnteil == null) {
			return null;
		}
		return newDroitRF(eigentumAnteil, ayantDroitProvider, communauteProvider, immeubleProvider);
	}
}
