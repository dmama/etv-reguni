package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.rechteregister.Beleg;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.StandardRecht;
import ch.vd.registre.base.date.RegDate;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
import ch.vd.unireg.registrefoncier.BeneficeServitudeRF;
import ch.vd.unireg.registrefoncier.ChargeServitudeRF;
import ch.vd.unireg.registrefoncier.DroitHabitationRF;
import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;
import ch.vd.unireg.registrefoncier.IdentifiantDroitRF;
import ch.vd.unireg.registrefoncier.ImmeubleRF;
import ch.vd.unireg.registrefoncier.ServitudeRF;
import ch.vd.unireg.registrefoncier.TiersRF;
import ch.vd.unireg.registrefoncier.UsufruitRF;
import ch.vd.unireg.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;
import ch.vd.unireg.registrefoncier.key.DroitRFKey;

import static ch.vd.unireg.registrefoncier.dataimport.helper.DroitRFHelper.masterIdAndVersionIdEquals;
import static ch.vd.unireg.registrefoncier.dataimport.helper.DroitRFHelper.numeroAffaireEquals;

public class ServitudesRFHelper {

	public static DroitRFKey newServitudeRFKey(DienstbarkeitExtendedElement droit) {
		return new DroitRFKey(droit.getDienstbarkeit().getMasterID(), droit.getDienstbarkeit().getVersionID());
	}


	public static boolean dataEquals(Set<ServitudeRF> servitudes, List<DienstbarkeitExtendedElement> dienstbarkeits) {

		//noinspection Duplicates
		if ((servitudes == null || servitudes.isEmpty()) && (dienstbarkeits == null || dienstbarkeits.isEmpty())) {
			// les deux collections sont vides ou nulles
			return true;
		}
		else if (servitudes == null || dienstbarkeits == null) {
			// une seule collection est vide ou nulle
			return false;
		}
		else if (servitudes.size() != dienstbarkeits.size()) {
			// les collections ne sont pas de tailles identiques
			return false;
		}

		List<ServitudeRF> remaining = new ArrayList<>(servitudes);
		for (DienstbarkeitExtendedElement d : dienstbarkeits) {
			boolean found = false;
			for (int i = 0; i < remaining.size(); i++) {
				ServitudeRF servitudeRF = remaining.get(i);
				if (dataEquals(servitudeRF, d)) {
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


	public static boolean dataEquals(ServitudeRF droitRF, DienstbarkeitExtendedElement dienstbarkeit) {
		return currentDataEquals(droitRF, get(dienstbarkeit, ServitudesRFHelper::simplisticAyantDroitProvider, ServitudesRFHelper::simplisticImmeubleProvider));
	}

	/**
	 * Provider d'ayant-droit simplifié au maximum pour retourner un ayant-droit avec juste l'idRF de renseigné.
	 */
	@NotNull
	private static AyantDroitRF simplisticAyantDroitProvider(String idRef) {
		final AyantDroitRF i = new TiersRF() {
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
	 * Compare les valeurs courantes des deux servitudes et retourne <i>vrai</i> si elles sont égales.
	 *
	 * @param left  une servitude
	 * @param right une autre servitude
	 * @return <i>vrai</i> si les valeurs courantes des deux servitudes sont égales; <i>faux</i> autrement.
	 */
	public static boolean currentDataEquals(@NotNull ServitudeRF left, @NotNull ServitudeRF right) {

		if (!masterIdAndVersionIdEquals(left, right)) {
			return false;
		}

		if (!left.getClass().equals(right.getClass())) {
			return false;
		}

		final boolean baseEquals = Objects.equals(left.getIdentifiantDroit(), right.getIdentifiantDroit()) &&
				numeroAffaireEquals(left.getNumeroAffaire(), right.getNumeroAffaire()) &&
				left.getDateDebutMetier() == right.getDateDebutMetier() &&
				left.getDateFinMetier() == right.getDateFinMetier() &&
				Objects.equals(left.getMotifDebut(), right.getMotifDebut()) &&
				Objects.equals(left.getMotifFin(), right.getMotifFin());
		if (!baseEquals) {
			return false;
		}

		// [SIFISC-27523] on vérifie que les listes d'ayants-droit et d'immeubles sont les mêmes
		return currentAyantDroitsEquals(left.getBenefices(), right.getBenefices()) &&
				currentImmeublesEquals(left.getCharges(), right.getCharges());
	}

	/**
	 * @param left  une collection de bénéficiaires de servitude (une collection nulle est assimilée à une collection vide)
	 * @param right une autre collection de bénéficiaires de servitude (une collection nulle est assimilée à une collection vide)
	 * @return <b>vrai</b> si les deux collections possèdent les mêmes de bénéficiaires de servitude courants (= sans date de fin et avec les mêmes ID RF); <b>faux</b> autrement.
	 */
	private static boolean currentAyantDroitsEquals(@Nullable Collection<BeneficeServitudeRF> left, @Nullable Collection<BeneficeServitudeRF> right) {

		final List<AyantDroitRF> leftList = (left == null ? Collections.emptyList() : left.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(l -> l.getDateFin() == null)
				.map(BeneficeServitudeRF::getAyantDroit)
				.collect(Collectors.toList()));
		final List<AyantDroitRF> rightList = (right == null ? Collections.emptyList() : right.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(l -> l.getDateFin() == null)
				.map(BeneficeServitudeRF::getAyantDroit)
				.collect(Collectors.toList()));

		if (leftList.size() != rightList.size()) {
			return false;
		}

		leftList.sort(Comparator.comparing(AyantDroitRF::getIdRF));
		rightList.sort(Comparator.comparing(AyantDroitRF::getIdRF));

		for (int i = 0; i < leftList.size(); i++) {
			final AyantDroitRF l = leftList.get(i);
			final AyantDroitRF r = rightList.get(i);
			if (!l.getIdRF().equals(r.getIdRF())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param left  une collection d'immeubles
	 * @param right une autre collection d'immeubles
	 * @return <b>vrai</b> si les deux collections possèdent les mêmes immeubles courants (= sans date de fin et les mêmes ID RF); <b>faux</b> autrement.
	 */
	private static boolean currentImmeublesEquals(@Nullable Collection<ChargeServitudeRF> left, @Nullable Collection<ChargeServitudeRF> right) {

		final List<ImmeubleRF> leftList = (left == null ? Collections.emptyList() : left.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(l -> l.getDateFin() == null)
				.map(ChargeServitudeRF::getImmeuble)
				.collect(Collectors.toList()));
		final List<ImmeubleRF> rightList = (right == null ? Collections.emptyList() : right.stream()
				.filter(AnnulableHelper::nonAnnule)
				.filter(r -> r.getDateFin() == null)
				.map(ChargeServitudeRF::getImmeuble)
				.collect(Collectors.toList()));

		if (leftList.size() != rightList.size()) {
			return false;
		}

		leftList.sort(Comparator.comparing(ImmeubleRF::getIdRF));
		rightList.sort(Comparator.comparing(ImmeubleRF::getIdRF));

		for (int i = 0; i < leftList.size(); i++) {
			final ImmeubleRF l = leftList.get(i);
			final ImmeubleRF r = rightList.get(i);
			if (!l.getIdRF().equals(r.getIdRF())) {
				return false;
			}
		}

		return true;
	}

	@NotNull
	public static ServitudeRF newServitudeRF(@NotNull DienstbarkeitExtendedElement dienstbarkeitExtended,
	                                         @NotNull Function<String, AyantDroitRF> ayantDroitProvider,
	                                         @NotNull Function<String, ImmeubleRF> immeubleProvider) {

		final Dienstbarkeit dienstbarkeit = dienstbarkeitExtended.getDienstbarkeit();
		final LastRechtGruppe lastRechtGruppe = dienstbarkeitExtended.getLastRechtGruppe();
		final String masterIdRF = dienstbarkeit.getMasterID();
		final String versionIdRF = dienstbarkeit.getVersionID();

		final String typeServitude = Optional.of(dienstbarkeit)
				.map(StandardRecht::getStichwort)
				.map(ch.vd.capitastra.rechteregister.CapiCode::getTextFr)
				.orElseThrow(() -> new IllegalArgumentException("La servitude standardRechtId=[" + masterIdRF + "] ne possède pas de type usufruit/droit d'habitation."));

		final ServitudeRF servitude;
		switch (typeServitude) {
		case "Usufruit":
			servitude = new UsufruitRF();
			break;
		case "Droit d'habitation":
			servitude = new DroitHabitationRF();
			break;
		default:
			throw new IllegalArgumentException("Type de servitude inconnue = [" + typeServitude + "]");
		}

		final RegDate dateDebutMetier = dienstbarkeit.getBeginDatum();
		final RegDate dateFinMetier = dienstbarkeit.getAblaufDatum();

		lastRechtGruppe.getBerechtigtePerson().forEach(person -> {
			final String personIDRef = DienstbarkeitExtendedElement.getPersonIDRef(person);
			final AyantDroitRF ayantDroit = ayantDroitProvider.apply(personIDRef);
			servitude.addBenefice(new BeneficeServitudeRF(dateDebutMetier, dateFinMetier, servitude, ayantDroit));
		});

		lastRechtGruppe.getBelastetesGrundstueck().forEach(grundstueck -> {
			final ImmeubleRF immeuble = immeubleProvider.apply(grundstueck.getBelastetesGrundstueckIDREF());
			servitude.addCharge(new ChargeServitudeRF(dateDebutMetier, dateFinMetier, servitude, immeuble));
		});

		servitude.setMasterIdRF(masterIdRF);
		servitude.setVersionIdRF(versionIdRF);
		servitude.setIdentifiantDroit(getIdentifiantDroit(dienstbarkeit));
		servitude.setNumeroAffaire(getAffaire(dienstbarkeit));
		servitude.setDateDebutMetier(dateDebutMetier);
		servitude.setDateFinMetier(dateFinMetier);
		servitude.setMotifDebut(null);
		servitude.setMotifFin(null);

		return servitude;
	}

	@NotNull
	private static IdentifiantDroitRF getIdentifiantDroit(@NotNull Dienstbarkeit dienstbarkeit) {
		return new IdentifiantDroitRF((int) dienstbarkeit.getAmtNummer(),
		                              (int) dienstbarkeit.getRechtEintragJahrID(),
		                              (int) dienstbarkeit.getRechtEintragNummerID());
	}

	@Nullable
	private static IdentifiantAffaireRF getAffaire(@Nullable Dienstbarkeit dienstbarkeit) {
		if (dienstbarkeit == null) {
			return null;
		}
		final Beleg beleg = dienstbarkeit.getBeleg();
		final String belegAsString = dienstbarkeit.getBelegAlt();
		if (beleg != null) {
			return new IdentifiantAffaireRF(beleg.getAmtNummer(), beleg.getBelegJahr(), beleg.getBelegNummer(), beleg.getBelegNummerIndex());
		}
		else if (belegAsString != null) {
			return new IdentifiantAffaireRF((int) dienstbarkeit.getAmtNummer(), belegAsString);
		}
		else {
			return null;
		}
	}

	@Nullable
	public static ServitudeRF get(@Nullable DienstbarkeitExtendedElement servitude,
	                              @NotNull Function<String, AyantDroitRF> ayantDroitProvider,
	                              @NotNull Function<String, ImmeubleRF> immeubleProvider) {
		if (servitude == null) {
			return null;
		}
		return newServitudeRF(servitude, ayantDroitProvider, immeubleProvider);
	}
}
