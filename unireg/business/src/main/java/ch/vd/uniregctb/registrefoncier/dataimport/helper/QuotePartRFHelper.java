package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.StammGrundstueck;
import ch.vd.uniregctb.registrefoncier.Fraction;
import ch.vd.uniregctb.registrefoncier.QuotePartRF;

public abstract class QuotePartRFHelper {

	private QuotePartRFHelper() {
	}


	public static boolean dataEquals(@Nullable QuotePartRF left, @Nullable QuotePartRF right) {
		if (left == null && right == null) {
			return true;
		}
		else //noinspection SimplifiableIfStatement
			if (left == null || right == null) {
				return false;
			}
			else {
				return Objects.equals(left.getQuotePart(), right.getQuotePart());
			}
	}

	@NotNull
	public static QuotePartRF newQuotePartRF(@NotNull Fraction fraction) {
		final QuotePartRF qp = new QuotePartRF();
		qp.setQuotePart(fraction);
		return qp;
	}

	@Nullable
	public static QuotePartRF get(@Nullable Fraction fraction) {
		if (fraction == null) {
			return null;
		}
		return newQuotePartRF(fraction);
	}

	@Nullable
	public static QuotePartRF get(@Nullable StammGrundstueck stammGrundstueck) {
		if (stammGrundstueck == null) {
			return null;
		}
		final Fraction fraction = FractionHelper.get(stammGrundstueck.getQuote());
		if (fraction == null) {
			return null;
		}
		return newQuotePartRF(fraction);
	}
}
