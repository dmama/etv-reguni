package ch.vd.unireg.registrefoncier.dataimport.helper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Rechtsgrund;
import ch.vd.unireg.common.AnnulableHelper;
import ch.vd.unireg.registrefoncier.RaisonAcquisitionRF;

import static ch.vd.unireg.registrefoncier.dataimport.helper.DroitRFHelper.getAffaire;
import static ch.vd.unireg.registrefoncier.dataimport.helper.DroitRFHelper.getMotif;
import static ch.vd.unireg.registrefoncier.dataimport.helper.DroitRFHelper.numeroAffaireEquals;

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

	/**
	 * @param left une collection de raisons d'acquisition
	 * @param right une autre collection de raisons d'acquisition
	 * @return vrai si les deux collections sont égales (les raisons d'acquisition annulées sont ignorées)
	 */
	public static boolean dataEquals(@Nullable Collection<RaisonAcquisitionRF> left, @Nullable Collection<RaisonAcquisitionRF> right) {

		final List<RaisonAcquisitionRF> leftList = (left == null ? Collections.emptyList() : left.stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toList()));
		final List<RaisonAcquisitionRF> rightList = (right == null ? Collections.emptyList() : right.stream()
				.filter(AnnulableHelper::nonAnnule)
				.collect(Collectors.toList()));

		if (leftList.size() != rightList.size()) {
			// les collections filtrées ne sont pas de tailles identiques
			return false;
		}

		List<RaisonAcquisitionRF> remaining = new ArrayList<>(leftList);
		for (RaisonAcquisitionRF r : rightList) {
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
