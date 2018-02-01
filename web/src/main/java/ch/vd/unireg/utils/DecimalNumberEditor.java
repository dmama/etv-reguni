package ch.vd.unireg.utils;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

public class DecimalNumberEditor extends PropertyEditorSupport {

	private static final Locale LOCALE = new Locale("fr", "CH");
	private static final DecimalFormatSymbols SYMBOLS = DecimalFormatSymbols.getInstance(LOCALE);
	private static final char GROUPING_SEPARATOR = SYMBOLS.getGroupingSeparator();
	private static final char DECIMAL_SEPARATOR = SYMBOLS.getDecimalSeparator();
	private static final char MINUS_PREFIX = SYMBOLS.getMinusSign();
	private static final String GROUPING_REMOVAL_REGEX = String.format("\\Q%c\\E", GROUPING_SEPARATOR);
	private static final Pattern PARSING_PATTERN = Pattern.compile(String.format("(?:\\Q%c\\E)?[0-9][0-9%c]*(?:\\Q%c\\E[0-9]+)?", MINUS_PREFIX, GROUPING_SEPARATOR, DECIMAL_SEPARATOR));

	private final NumberFormat numberFormat;

	private static NumberFormat buildNumberFormat(int fractionDigits) {
		final NumberFormat nf = NumberFormat.getInstance(LOCALE);
		nf.setMinimumFractionDigits(fractionDigits);
		nf.setMaximumFractionDigits(fractionDigits);
		nf.setMinimumIntegerDigits(1);
		return nf;
	}

	public DecimalNumberEditor(int fractionDigits) {
		this.numberFormat = buildNumberFormat(fractionDigits);
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.isBlank(text)) {
			setValue(null);
		}
		else {
			final String trimmed = text.trim();
			final Matcher matcher = PARSING_PATTERN.matcher(trimmed);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("Cannot parse decimal value: not composed of digits, out of bounds...");
			}

			final String pure = trimmed.replaceAll(GROUPING_REMOVAL_REGEX, StringUtils.EMPTY);
			final BigDecimal value = new BigDecimal(pure);
			setValue(value);
		}
	}

	@Override
	public String getAsText() {
		final Number value = (Number) getValue();
		synchronized (numberFormat) {
			return value != null ? numberFormat.format(value) : StringUtils.EMPTY;
		}
	}
}
