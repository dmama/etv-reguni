package ch.vd.uniregctb.registrefoncier.helper;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.AmtlicheBewertung;
import ch.vd.uniregctb.registrefoncier.EstimationRF;

public abstract class EstimationRFHelper {

	private EstimationRFHelper() {
	}

	public static boolean estimationEquals(@NotNull EstimationRF estimation, @NotNull AmtlicheBewertung amtlicheBewertung) {

		if (!Objects.equals(amtlicheBewertung.getAmtlicherWert(), estimation.getMontant())) {
			// le montant est différent
			return false;
		}

		if (!Objects.equals(amtlicheBewertung.getProtokollNr(), estimation.getReference())) {
			// la référence est différente
			return false;
		}

		if (!Objects.equals(amtlicheBewertung.getProtokollDatum(), estimation.getDateEstimation())) {
			// la date d'estimation est différente
			return false;
		}

		if (!Objects.equals(amtlicheBewertung.isProtokollGueltig(), !estimation.isEnRevision())) {
			// le flag en révision est différent
			return false;
		}

		return true;
	}
}
