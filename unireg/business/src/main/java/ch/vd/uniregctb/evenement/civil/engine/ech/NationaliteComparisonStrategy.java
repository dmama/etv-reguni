package ch.vd.uniregctb.evenement.civil.engine.ech;

import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.uniregctb.common.DataHolder;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

/**
 * Comparateur d'individu basé sur les nationalités de l'individu
 */
public class NationaliteComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUT = "nationalité";

	private static enum StatutNationalite {
		AUCUNE,
		INCONNUE,
		APATRIDE,
		CONNUE
	}

	private static StatutNationalite getNationalityStatus(List<Nationalite> liste) {
		boolean foundInconnue = false;
		boolean foundApatride = false;
		for (Nationalite nat : liste) {
			final int ofs = nat.getPays().getNoOFS();
			if (ofs == ServiceInfrastructureService.noPaysInconnu) {
				foundInconnue = true;
			}
			else if (ofs == ServiceInfrastructureService.noPaysApatride) {
				foundApatride = true;
			}
			else {
				return StatutNationalite.CONNUE;
			}
		}
		if (foundApatride) {
			return StatutNationalite.APATRIDE;
		}
		else if (foundInconnue) {
			return StatutNationalite.INCONNUE;
		}
		return StatutNationalite.AUCUNE;
	}

	@Nullable
	private static Nationalite getNationaliteSuisse(List<Nationalite> liste) {
		for (Nationalite n : liste) {
			if (n.getPays().isSuisse()) {
				return n;
			}
		}
		return null;
	}

	@Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull DataHolder<String> msg) {

		// La spécification dit qu'il faut détecter le passage de "nationalité inconnue" à "nationalité connue" et vice-versa (= statut de la nationalité),
		// et que dans le cas où les nationalités existaient et changent, seule les changements sur la nationalité suisse sont importants
		final List<Nationalite> origNationalites = originel.getIndividu().getNationalites();
		final List<Nationalite> corNationalites = corrige.getIndividu().getNationalites();
		final StatutNationalite origStatus = getNationalityStatus(origNationalites);
		final StatutNationalite corStatus = getNationalityStatus(corNationalites);
		boolean sansImpact = origStatus == corStatus;
		if (sansImpact) {
			// filtrons les nationalités étrangères pour ne garder que la nationalité suisse et comparons les dates
			final Nationalite origSuisse = getNationaliteSuisse(origNationalites);
			final Nationalite corSuisse = getNationaliteSuisse(corNationalites);
			sansImpact = IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(origSuisse, corSuisse);
		}

		if (!sansImpact) {
			msg.set(ATTRIBUT);
		}
		return sansImpact;
	}
}
