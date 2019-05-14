package ch.vd.unireg.utils;

import java.beans.PropertyEditorSupport;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.common.UniregLocale;

public class DecimalNumberEditor extends PropertyEditorSupport {

	private static final char GROUPING_SEPARATOR = UniregLocale.SYMBOLS.getGroupingSeparator();
	private static final char DECIMAL_SEPARATOR = UniregLocale.SYMBOLS.getDecimalSeparator();
	private static final char MINUS_PREFIX = UniregLocale.SYMBOLS.getMinusSign();
	private static final String GROUPING_REMOVAL_REGEX = String.format("\\Q%c\\E", GROUPING_SEPARATOR);

	// exemples de chiffres valides :
	// -123
	// 123.45
	// 123'456
	// 123'456.78
	// 111'222'333
	private static final String PATTERN_FORMAT = String.format("(?:\\Q%c\\E)?[0-9][0-9%c]*(?:\\Q%c\\E[0-9]+)?", MINUS_PREFIX, GROUPING_SEPARATOR, DECIMAL_SEPARATOR);
	private static final Pattern PARSING_PATTERN = Pattern.compile(PATTERN_FORMAT);

	private final NumberFormat numberFormat;

	private static NumberFormat buildNumberFormat(int fractionDigits) {
		final NumberFormat nf = NumberFormat.getInstance(UniregLocale.LOCALE);
		((DecimalFormat) nf).setDecimalFormatSymbols(UniregLocale.SYMBOLS);
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
