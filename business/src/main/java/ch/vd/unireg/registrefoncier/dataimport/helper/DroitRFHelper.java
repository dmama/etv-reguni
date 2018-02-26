package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.ArrayList;
import java.util.Collections;
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
import ch.vd.registre.base.date.DateRange;
import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.date.NullDateBehavior;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.CommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteCommunauteRF;
import ch.vd.unireg.registrefoncier.DroitProprieteImmeubleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonneMoraleRF;
import ch.vd.unireg.registrefoncier.DroitProprietePersonnePhysiqueRF;
import ch.vd.unireg.registrefoncier.DroitProprieteRF;
import ch.vd.unireg.registrefoncier.DroitRF;
import ch.vd.unireg.registrefoncier.Fraction;
import ch.vd.unireg.registrefoncier.GenrePropriete;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;

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
		return dataEquals(droitRF, get(eigentumAnteil,
		                               DroitRFHelper::simplisticAyantDroitProvider,
		                               DroitRFHelper::simplisticCommunauteProvider,
		                               DroitRFHelper::simplisticImmeubleProvider));
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

	@Nullable
	private static DroitProprieteRF simplisticDroitPrecedentProvider(@NotNull DroitProprieteRF key) {
		return null;
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
				// notes :
				//  - la date de début métiers et le motif de début sont déduits des raisons d'acquisition, on ne les compare pas.
				//  - la date de fin métier n'est pas renseignée dans le fichier d'entrée, on ne la compare pas
				RaisonAcquisitionRFHelper.dataEquals(left.getRaisonsAcquisition(), right.getRaisonsAcquisition());
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

	public static String getImmeubleIdRF(@NotNull EigentumAnteil eigentumAnteil) {
		return eigentumAnteil.getBelastetesGrundstueckIDREF();
	}

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

	public static DroitProprieteRF get(@Nullable EigentumAnteil eigentumAnteil,
	                                   @NotNull Function<String, AyantDroitRF> ayantDroitProvider,
	                                   @NotNull Function<String, CommunauteRF> communauteProvider,
	                                   @NotNull Function<String, ImmeubleRF> immeubleProvider) {
		if (eigentumAnteil == null) {
			return null;
		}
		return newDroitRF(eigentumAnteil, ayantDroitProvider, communauteProvider, immeubleProvider);
	}

	public static class DroitIntersection implements DateRange {
		private final RegDate dateDebut;
		private final RegDate dateFin;
		private final String motifDebut;
		private final String motifFin;

		public DroitIntersection(RegDate dateDebut, RegDate dateFin, String motifDebut, String motifFin) {
			this.dateDebut = dateDebut;
			this.dateFin = dateFin;
			this.motifDebut = motifDebut;
			this.motifFin = motifFin;
		}

		public RegDate getDateDebut() {
			return dateDebut;
		}

		public RegDate getDateFin() {
			return dateFin;
		}

		public String getMotifDebut() {
			return motifDebut;
		}

		public String getMotifFin() {
			return motifFin;
		}
	}

	/**
	 * Calcul l'intersection de deux droits selon leurs dates <b>métier</b> et retourne le résultat. Les dates nulles sont évaluées comme :
	 * <ul>
	 * <li>pour les dates de début comme la nuit des temps (Big Bang)</li>
	 * <li>pour les dates de fin comme la fin des temps (Big Crunch)</li>
	 * </ul>
	 *
	 * @param d1 le premier droit
	 * @param d2 le second droit
	 * @return l'intersection des deux droits selon leurs dates <b>métier</b>, ou <b>null</b> si les deux ranges n'ont rien en commun.
	 */
	public static DroitIntersection intersection(@NotNull DroitRF d1, @NotNull DroitRF d2) {

		final DateRange r1 = d1.getRangeMetier();
		final DateRange r2 = d2.getRangeMetier();

		if (DateRangeHelper.intersect(r1, r2)) {
			// Il y a une intersection
			final RegDate dateDebut;
			final String motifDebut;
			if (NullDateBehavior.EARLIEST.compare(r1.getDateDebut(), r2.getDateDebut()) >= 0) {
				dateDebut = r1.getDateDebut();
				motifDebut = d1.getMotifDebut();
			}
			else {
				dateDebut = r2.getDateDebut();
				motifDebut = d2.getMotifDebut();
			}
			final RegDate dateFin;
			final String motifFin;
			if (NullDateBehavior.LATEST.compare(r1.getDateFin(), r2.getDateFin()) <= 0) {
				dateFin = r1.getDateFin();
				motifFin = d1.getMotifFin();
			}
			else {
				dateFin = r2.getDateFin();
				motifFin = d2.getMotifFin();
			}
			return new DroitIntersection(dateDebut, dateFin, motifDebut, motifFin);
		}
		else {
			// Pas d'intersection
			return null;
		}
	}

	/**
	 * Interface permettant d'adapter un droit en fonction de nouvelles valeurs de dates de début/fin.
	 */
	public interface AdapterCallback<T extends DroitRF> {
		/**
		 * Retourne une nouvelles instance de T adaptée au dates de début et de fin spécifié.
		 *
		 * @param range le range à adapter
		 * @param debut la nouvelle date de début, ou <b>null</b> s'il ne faut pas adapter la date de début.
		 * @param fin   la nouvelle date de fin, ou <b>null</b> s'il ne faut pas adapter la date de fin.
		 * @return une nouvelle instance adaptée aux dates spécifiées.
		 */
		DroitRF adapt(T range, RegDate debut, RegDate fin);
	}

	/**
	 * Equivalent de la méthode {@link DateRangeHelper#extract(List, List, DateRangeHelper.AdapterCallback)} spécialisée pour les droits RF.
	 *
	 * @param droits  une liste de droits
	 * @param ranges  une liste de ranges
	 * @param adapter l'adapteur pour créer les nouveaux droits
	 * @return lea droits extraits
	 */
	public static <T extends DroitRF> List<DroitRF> extract(List<T> droits, List<? extends DateRange> ranges, AdapterCallback<T> adapter) {

		if (droits == null || droits.isEmpty() || ranges == null || ranges.isEmpty()) {
			return Collections.emptyList();
		}

		final List<DroitRF> list = new ArrayList<>();

		for (T d : droits) {
			final DateRange droitRange = d.getRangeMetier();
			for (DateRange r : ranges) {

				final DateRange intersection = DateRangeHelper.intersection(droitRange, r);
				if (intersection != null) {

					final RegDate interDebut = intersection.getDateDebut();
					final RegDate interFin = intersection.getDateFin();

					// On adapte le début/fin que si nécessaire
					final RegDate debut = (interDebut == droitRange.getDateDebut() ? null : interDebut);
					final RegDate fin = (interFin == droitRange.getDateFin() ? null : interFin);
					list.add(adapter.adapt(d, debut, fin));
				}
			}
		}

		return list;
	}

	/**
	 * Equivalent de la méthode {@link DateRangeHelper#extract(List, List, DateRangeHelper.AdapterCallback)} spécialisée pour un seul droit RF.
	 *
	 * @param droit  un droit
	 * @param range  un range
	 * @param adapter l'adapteur pour créer les nouveaux droits
	 * @return le droit extrait
	 */
	@Nullable
	public static <T extends DroitRF> DroitRF extract(T droit, DateRange range, AdapterCallback<T> adapter) {

		if (droit == null || range == null) {
			return null;
		}

		final DateRange droitRange = droit.getRangeMetier();

		final DateRange intersection = DateRangeHelper.intersection(droitRange, range);
		if (intersection == null) {
			return null;
		}

		final RegDate interDebut = intersection.getDateDebut();
		final RegDate interFin = intersection.getDateFin();

		// On adapte le début/fin que si nécessaire
		final RegDate debut = (interDebut == droitRange.getDateDebut() ? null : interDebut);
		final RegDate fin = (interFin == droitRange.getDateFin() ? null : interFin);
		return adapter.adapt(droit, debut, fin);
	}
}
