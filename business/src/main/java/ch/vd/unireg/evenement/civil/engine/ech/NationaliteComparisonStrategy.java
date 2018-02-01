package ch.vd.unireg.evenement.civil.engine.ech;

import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.unireg.interfaces.civil.data.Nationalite;
import ch.vd.unireg.common.NationaliteHelper;
import ch.vd.unireg.interfaces.service.ServiceInfrastructureService;

/**
 * Comparateur d'individu basé sur les nationalités de l'individu
 */
public class NationaliteComparisonStrategy implements IndividuComparisonStrategy {

	private static final String ATTRIBUT = "nationalité";
	private static final String DATES = "dates";

	private enum StatutNationalite {
		AUCUNE,
		INCONNUE,
		APATRIDE,
		CONNUE
	}

	private static StatutNationalite getNationalityStatus(Nationalite nat) {
		if (nat == null) {
			return StatutNationalite.AUCUNE;
		}
		final int ofs = nat.getPays().getNoOFS();
		if ((ofs == ServiceInfrastructureService.noPaysApatride)) {
			return StatutNationalite.APATRIDE;
		}
		else if ((ofs == ServiceInfrastructureService.noPaysInconnu)) {
			return StatutNationalite.INCONNUE;
		}
		return StatutNationalite.CONNUE;
	}

	@Nullable
	private static Nationalite getNationaliteSuisse(@Nullable Nationalite n) {
		if (n != null && n.getPays().isSuisse()) {
			return n;
		}
		return null;
	}

	@Override
	public boolean isFiscalementNeutre(IndividuApresEvenement originel, IndividuApresEvenement corrige, @NotNull Mutable<String> msg) {

		// La spécification dit qu'il faut détecter le passage de "nationalité inconnue" à "nationalité connue" et vice-versa (= statut de la nationalité),
		// et que dans le cas où les nationalités existaient et changent, seuls les changements sur la nationalité suisse sont importants
		final Nationalite origNationalite = NationaliteHelper.refAt(originel.getIndividu().getNationalites(), originel.getDateEvenement());
		final Nationalite corNationalite = NationaliteHelper.refAt(corrige.getIndividu().getNationalites(), corrige.getDateEvenement());
		final StatutNationalite origStatus = getNationalityStatus(origNationalite);
		final StatutNationalite corStatus = getNationalityStatus(corNationalite);
		final IndividuComparisonHelper.FieldMonitor monitor = new IndividuComparisonHelper.FieldMonitor();
		boolean neutre = true;
		if (origStatus != corStatus) {
			if (origStatus == StatutNationalite.AUCUNE || corStatus == StatutNationalite.AUCUNE) {
				IndividuComparisonHelper.fillMonitorWithApparitionDisparition(origStatus == StatutNationalite.AUCUNE, monitor, ATTRIBUT);
			}
			else {
				IndividuComparisonHelper.fillMonitor(monitor, ATTRIBUT);
			}
			neutre = false;
		}
		else {
			// filtrons les nationalités étrangères pour ne garder que la nationalité suisse et comparons les dates
			final Nationalite origSuisse = getNationaliteSuisse(origNationalite);
			final Nationalite corSuisse = getNationaliteSuisse(corNationalite);
			if (origSuisse != null && corSuisse != null) {
				neutre = IndividuComparisonHelper.RANGE_EQUALATOR.areEqual(origSuisse, corSuisse, monitor, DATES);
			}
			else if (origSuisse != null || corSuisse != null) {
				neutre = false;
			}
			if (!neutre) {
				IndividuComparisonHelper.fillMonitor(monitor, ATTRIBUT);
			}
		}
		if (!neutre) {
			msg.setValue(IndividuComparisonHelper.buildErrorMessage(monitor));
		}
		return neutre;
	}
}
