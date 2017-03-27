package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.uniregctb.registrefoncier.RaisonAcquisitionRF;

import static ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper.getAffaire;
import static ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper.getMotif;
import static ch.vd.uniregctb.registrefoncier.dataimport.helper.DroitRFHelper.numeroAffaireEquals;

public abstract class RaisonAcquisitionRFHelper {

	private RaisonAcquisitionRFHelper() {
	}

	public static boolean dataEquals(Set<RaisonAcquisitionRF> raisons, List<Rechtsgrund> rechtsgrunds) {

		//noinspection Duplicates
		if ((raisons == null || raisons.isEmpty()) && (rechtsgrunds == null || rechtsgrunds.isEmpty())) {
			// les deux collections sont vides ou nulles
			return true;
		}
		else if (raisons == null || rechtsgrunds == null) {
			// une seule collection est vide ou nulle
			return false;
		}
		else if (raisons.size() != rechtsgrunds.size()) {
			// les collections ne sont pas de tailles identiques
			return false;
		}

		List<RaisonAcquisitionRF> remaining = new ArrayList<>(raisons);
		for (Rechtsgrund r : rechtsgrunds) {
			boolean found = false;
			for (int i = 0; i < remaining.size(); i++) {
				RaisonAcquisitionRF raison = remaining.get(i);
				if (dataEquals(raison, r)) {
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

	public static boolean dataEquals(Collection<RaisonAcquisitionRF> left, Collection<RaisonAcquisitionRF> right) {

		//noinspection Duplicates
		if ((left == null || left.isEmpty()) && (right == null || right.isEmpty())) {
			// les deux collections sont vides ou nulles
			return true;
		}
		else if (left == null || right == null) {
			// une seule collection est vide ou nulle
			return false;
		}
		else if (left.size() != right.size()) {
			// les collections ne sont pas de tailles identiques
			return false;
		}

		List<RaisonAcquisitionRF> remaining = new ArrayList<>(left);
		for (RaisonAcquisitionRF r : right) {
			boolean found = false;
			for (int i = 0; i < remaining.size(); i++) {
				RaisonAcquisitionRF raison = remaining.get(i);
				if (dataEquals(raison, r)) {
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

	private static boolean dataEquals(@NotNull RaisonAcquisitionRF raison, @NotNull Rechtsgrund rechtsgrund) {
		return dataEquals(raison, get(rechtsgrund));
	}

	public static boolean dataEquals(@NotNull RaisonAcquisitionRF left, @NotNull RaisonAcquisitionRF right) {
		return left.getDateAcquisition() == right.getDateAcquisition() &&
				Objects.equals(left.getMotifAcquisition(), right.getMotifAcquisition()) &&
				numeroAffaireEquals(left.getNumeroAffaire(), right.getNumeroAffaire());
	}

	@Nullable
	public static RaisonAcquisitionRF get(@Nullable Rechtsgrund rechtsgrund) {
		if (rechtsgrund == null) {
			return null;
		}
		return newRaisonAcquisition(rechtsgrund);
	}

	@NotNull
	public static RaisonAcquisitionRF newRaisonAcquisition(@NotNull Rechtsgrund rechtsgrund) {
		final RaisonAcquisitionRF raison = new RaisonAcquisitionRF();
		raison.setDateAcquisition(rechtsgrund.getBelegDatum());
		raison.setMotifAcquisition(getMotif(rechtsgrund.getRechtsgrundCode()));
		raison.setNumeroAffaire(getAffaire(rechtsgrund));
		return raison;
	}
}
