package ch.vd.uniregctb.registrefoncier.helper;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.AmtlicheBewertung;
import ch.vd.uniregctb.registrefoncier.EstimationRF;

public abstract class EstimationRFHelper {

	private EstimationRFHelper() {
	}

	public static boolean dataEquals(@NotNull EstimationRF estimation, @NotNull AmtlicheBewertung amtlicheBewertung) {
		return dataEquals(estimation, newEstimationRF(amtlicheBewertung));
	}

	public static boolean dataEquals(@NotNull EstimationRF left, @NotNull EstimationRF right) {
		return Objects.equals(left.getMontant(), right.getMontant()) &&
				Objects.equals(left.getReference(), right.getReference()) &&
				Objects.equals(left.getDateEstimation(), right.getDateEstimation()) &&
				left.isEnRevision() == right.isEnRevision();
	}

	@NotNull
	public static EstimationRF newEstimationRF(@NotNull AmtlicheBewertung amtlicheBewertung) {
		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(amtlicheBewertung.getAmtlicherWert());
		estimation.setReference(amtlicheBewertung.getProtokollNr());
		estimation.setDateEstimation(amtlicheBewertung.getProtokollDatum());
		final Boolean gueltig = amtlicheBewertung.isProtokollGueltig();
		estimation.setEnRevision(gueltig == null || !gueltig);
		return estimation;
	}
}
