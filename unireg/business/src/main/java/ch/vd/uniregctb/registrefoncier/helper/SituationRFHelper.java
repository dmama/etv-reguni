package ch.vd.uniregctb.registrefoncier.helper;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.uniregctb.registrefoncier.SituationRF;

public abstract class SituationRFHelper {

	private SituationRFHelper() {
	}

	public static boolean dataEquals(@NotNull SituationRF situation, @NotNull GrundstueckNummer grundstueckNummer) {
		return dataEquals(situation, newSituationRF(grundstueckNummer));
	}

	public static boolean dataEquals(@NotNull SituationRF left, @NotNull SituationRF right) {
		return left.getNoOfsCommune() == right.getNoOfsCommune() &&
				left.getNoRfCommune() == right.getNoRfCommune() &&
				left.getNoParcelle() == right.getNoParcelle() &&
				Objects.equals(left.getIndex1(), right.getIndex1()) &&
				Objects.equals(left.getIndex2(), right.getIndex2()) &&
				Objects.equals(left.getIndex3(), right.getIndex3());
	}

	@NotNull
	public static SituationRF newSituationRF(@NotNull GrundstueckNummer grundstueckNummer) {
		final SituationRF situation = new SituationRF();
		situation.setNoRfCommune(grundstueckNummer.getBfsNr());
		situation.setNoParcelle(grundstueckNummer.getStammNr());
		situation.setIndex1(grundstueckNummer.getIndexNr1());
		situation.setIndex2(grundstueckNummer.getIndexNr2());
		situation.setIndex3(grundstueckNummer.getIndexNr3());
		return situation;
	}
}
