package ch.vd.uniregctb.registrefoncier.dataimport.helper;

import java.util.regex.Pattern;

import org.jetbrains.annotations.Nullable;

import ch.vd.capitastra.grundstueck.Quote;
import ch.vd.uniregctb.registrefoncier.Fraction;

public abstract class FractionHelper {
	private FractionHelper() {
	}

	public static boolean dataEquals(@Nullable Fraction left, @Nullable Quote right) {
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

	// support des fractions en pourcent (e.g. "2%", "15%", ...)
	private static final Pattern PATTERN_FRACTION_PERCENT = Pattern.compile("[0-9]?[0-9]%");

	@Nullable
	public static Fraction get(@Nullable Quote quote) {

		if (quote == null) {
			return null;
		}

		final Long numerateur = quote.getAnteilZaehler();
		final Long denominateur = quote.getAnteilNenner();
		final String quoteInProsa = quote.getQuoteInProsa();

		if (numerateur != null && denominateur != null) {
			return new Fraction(numerateur.intValue(), denominateur.intValue());
		}
		else if (quoteInProsa != null && PATTERN_FRACTION_PERCENT.matcher(quoteInProsa).matches()) {
			final String percentAsString = quoteInProsa.substring(0, quoteInProsa.length() - 1);    // suppression du pourcent
			return new Fraction(Integer.parseInt(percentAsString), 100);
		}
		else {
			return null;
		}
	}
}
