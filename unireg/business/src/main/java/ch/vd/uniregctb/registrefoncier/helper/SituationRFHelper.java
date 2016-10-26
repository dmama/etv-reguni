package ch.vd.uniregctb.registrefoncier.helper;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;

import ch.vd.capitastra.grundstueck.GrundstueckNummer;
import ch.vd.uniregctb.registrefoncier.SituationRF;

public abstract class SituationRFHelper {

	private SituationRFHelper() {
	}

	public static boolean situationEquals(@NotNull SituationRF situation, @NotNull GrundstueckNummer grundstueckNummer) {

		if (grundstueckNummer.getBfsNr() != situation.getNoRfCommune()) {
			// la commune diffère
			return false;
		}

		if (grundstueckNummer.getStammNr() != situation.getNoParcelle()) {
			// le numéro de parcelle diffère
			return false;
		}

		if (!Objects.equals(grundstueckNummer.getIndexNr1(), situation.getIndex1()) ||
				!Objects.equals(grundstueckNummer.getIndexNr2(), situation.getIndex2()) ||
				!Objects.equals(grundstueckNummer.getIndexNr3(), situation.getIndex3())) {
			// un des indexes diffère
			return false;
		}

		return true;
	}
}
