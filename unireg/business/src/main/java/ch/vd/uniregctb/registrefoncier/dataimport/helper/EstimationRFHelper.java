package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.AmtlicheBewertung;
import ch.vd.uniregctb.registrefoncier.EstimationRF;

public abstract class EstimationRFHelper {

	private EstimationRFHelper() {
	}

	public static boolean dataEquals(@Nullable EstimationRF estimation, @Nullable AmtlicheBewertung amtlicheBewertung) {
		return dataEquals(estimation, get(amtlicheBewertung));
	}

	public static boolean dataEquals(@Nullable EstimationRF left, @Nullable EstimationRF right) {
		if (left == null && right == null) {
			return true;
		}
		else //noinspection SimplifiableIfStatement
			if (left == null || right == null) {
			return false;
		}
		else {
			return Objects.equals(left.getMontant(), right.getMontant()) &&
					Objects.equals(left.getReference(), right.getReference()) &&
					Objects.equals(left.getDateInscription(), right.getDateInscription()) &&
					left.isEnRevision() == right.isEnRevision();
		}
	}

	@NotNull
	public static EstimationRF newEstimationRF(@NotNull AmtlicheBewertung amtlicheBewertung) {
		final EstimationRF estimation = new EstimationRF();
		estimation.setMontant(amtlicheBewertung.getAmtlicherWert());
		estimation.setReference(amtlicheBewertung.getProtokollNr());
		estimation.setDateInscription(amtlicheBewertung.getProtokollDatum());
		final Boolean gueltig = amtlicheBewertung.isProtokollGueltig();
		estimation.setEnRevision(gueltig == null || !gueltig);
		return estimation;
	}

	@Nullable
	public static EstimationRF get(@Nullable AmtlicheBewertung amtlicheBewertung) {
		if (amtlicheBewertung == null) {
			return null;
		}
		return newEstimationRF(amtlicheBewertung);
	}
}
