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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.rechteregister.Beleg;
import ch.vd.capitastra.rechteregister.Dienstbarkeit;
import ch.vd.capitastra.rechteregister.LastRechtGruppe;
import ch.vd.capitastra.rechteregister.StandardRecht;
import ch.vd.unireg.registrefoncier.AyantDroitRF;
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
		return dataEquals(droitRF, get(dienstbarkeit, ServitudesRFHelper::simplisticAyantDroitProvider, ServitudesRFHelper::simplisticImmeubleProvider));
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

	public static boolean dataEquals(@NotNull ServitudeRF left, @NotNull ServitudeRF right) {

		if (!masterIdAndVersionIdEquals(left, right)) {
			return false;
		}

		//noinspection SimplifiableIfStatement
		if (!left.getClass().equals(right.getClass())) {
			return false;
		}

		return equalsServitude(left, right);
	}

	private static boolean equalsServitude(@NotNull ServitudeRF left, @NotNull ServitudeRF right) {

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
		return ayantDroitsEquals(left.getAyantDroits(), right.getAyantDroits()) &&
				immeublesEquals(left.getImmeubles(), right.getImmeubles());
	}

	/**
	 * @param left  une collection d'ayants-droits (une collection nulle est assimilée à une collection vide)
	 * @param right une autre collection d'ayants-droits (une collection nulle est assimilée à une collection vide)
	 * @return <b>vrai</b> si les deux collections possèdent les mêmes ayants-droits (= les mêmes ID RF); <b>faux</b> autrement.
	 */
	private static boolean ayantDroitsEquals(@Nullable Collection<AyantDroitRF> left, @Nullable Collection<AyantDroitRF> right) {

		if (left == null) {
			left = Collections.emptyList();
		}
		if (right == null) {
			right = Collections.emptyList();
		}

		if (left.size() != right.size()) {
			return false;
		}

		final List<AyantDroitRF> sortedLeft = new ArrayList<>(left);
		sortedLeft.sort(Comparator.comparing(AyantDroitRF::getIdRF));

		final List<AyantDroitRF> sortedRight = new ArrayList<>(right);
		sortedRight.sort(Comparator.comparing(AyantDroitRF::getIdRF));

		for (int i = 0; i < sortedLeft.size(); i++) {
			final AyantDroitRF l = sortedLeft.get(i);
			final AyantDroitRF r = sortedRight.get(i);
			if (!l.getIdRF().equals(r.getIdRF())) {
				return false;
			}
		}

		return true;
	}

	/**
	 * @param left  une collection d'immeubles
	 * @param right une autre collection d'immeubles
	 * @return <b>vrai</b> si les deux collections possèdent les mêmes immeubles (= les mêmes ID RF); <b>faux</b> autrement.
	 */
	private static boolean immeublesEquals(@NotNull Collection<ImmeubleRF> left, @NotNull Collection<ImmeubleRF> right) {

		if (left.size() != right.size()) {
			return false;
		}

		final List<ImmeubleRF> sortedLeft = new ArrayList<>(left);
		sortedLeft.sort(Comparator.comparing(ImmeubleRF::getIdRF));

		final List<ImmeubleRF> sortedRight = new ArrayList<>(right);
		sortedRight.sort(Comparator.comparing(ImmeubleRF::getIdRF));

		for (int i = 0; i < sortedLeft.size(); i++) {
			final ImmeubleRF l = sortedLeft.get(i);
			final ImmeubleRF r = sortedRight.get(i);
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

		lastRechtGruppe.getBerechtigtePerson().forEach(person -> {
			final String personIDRef = DienstbarkeitExtendedElement.getPersonIDRef(DienstbarkeitExtendedElement.getPerson(person));
			servitude.addAyantDroit(ayantDroitProvider.apply(personIDRef));
		});

		lastRechtGruppe.getBelastetesGrundstueck().forEach(grundstueck -> {
			servitude.addImmeuble(immeubleProvider.apply(grundstueck.getBelastetesGrundstueckIDREF()));
		});

		servitude.setMasterIdRF(masterIdRF);
		servitude.setVersionIdRF(versionIdRF);
		servitude.setIdentifiantDroit(getIdentifiantDroit(dienstbarkeit));
		servitude.setNumeroAffaire(getAffaire(dienstbarkeit));
		servitude.setDateDebutMetier(dienstbarkeit.getBeginDatum());
		servitude.setDateFinMetier(dienstbarkeit.getAblaufDatum());
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
