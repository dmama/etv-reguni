package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.ArrayList;
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
import ch.vd.uniregctb.registrefoncier.AyantDroitRF;
import ch.vd.uniregctb.registrefoncier.DroitHabitationRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantAffaireRF;
import ch.vd.uniregctb.registrefoncier.IdentifiantDroitRF;
import ch.vd.uniregctb.registrefoncier.ImmeubleRF;
import ch.vd.uniregctb.registrefoncier.ServitudeRF;
import ch.vd.uniregctb.registrefoncier.TiersRF;
import ch.vd.uniregctb.registrefoncier.UsufruitRF;
import ch.vd.uniregctb.registrefoncier.dataimport.elements.servitude.DienstbarkeitExtendedElement;
import ch.vd.uniregctb.registrefoncier.key.DroitRFKey;

import static ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper.masterIdAndVersionIdEquals;
import static ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper.numeroAffaireEquals;

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
		return Objects.equals(left.getIdentifiantDroit(), right.getIdentifiantDroit()) &&
				numeroAffaireEquals(left.getNumeroAffaire(), right.getNumeroAffaire()) &&
				left.getDateDebutMetier() == right.getDateDebutMetier() &&
				left.getDateFinMetier() == right.getDateFinMetier() &&
				Objects.equals(left.getMotifDebut(), right.getMotifDebut()) &&
				Objects.equals(left.getMotifFin(), right.getMotifFin());
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
