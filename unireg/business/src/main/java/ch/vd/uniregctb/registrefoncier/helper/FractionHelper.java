package ch.vd.uniregctb.registrefoncier.helper;

import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.uniregctb.registrefoncier.Fraction;

public abstract class FractionHelper {
	private FractionHelper() {
	}

	public static boolean fractionEquals(@Nullable Fraction left, @Nullable Quote right) {
		if (right == null && left == null) {
			return true;
		}
		else if ((right == null) != (left == null)) {
			return false;
		}
		else {
			return right.getAnteilZaehler().equals((long) left.getNumerateur()) &&
					right.getAnteilNenner().equals((long) left.getDenominateur());
		}
	}
}
