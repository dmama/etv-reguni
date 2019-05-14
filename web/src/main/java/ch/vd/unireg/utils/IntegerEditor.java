package ch.vd.unireg.utils;

import java.beans.PropertyEditorSupport;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.common.UniregLocale;

public class IntegerEditor extends PropertyEditorSupport {

	private static final NumberFormat NUMBER_FORMAT;
	static {
		NUMBER_FORMAT = NumberFormat.getIntegerInstance(UniregLocale.LOCALE);
		((DecimalFormat) NUMBER_FORMAT).setDecimalFormatSymbols(UniregLocale.SYMBOLS);
	}

	private static final char GROUPING_SEPARATOR = UniregLocale.SYMBOLS.getGroupingSeparator();
	private static final char MINUS_PREFIX = UniregLocale.SYMBOLS.getMinusSign();
	private static final String GROUPING_REMOVAL_REGEX = String.format("\\Q%c\\E", GROUPING_SEPARATOR);

	private final Pattern acceptedInputPattern;

	public IntegerEditor(boolean onlyPositive) {
		this.acceptedInputPattern = onlyPositive
				? Pattern.compile(String.format("[0-9][0-9%c]*", GROUPING_SEPARATOR))
				: Pattern.compile(String.format("(?:\\Q%c\\E)?[0-9][0-9%c]*", MINUS_PREFIX, GROUPING_SEPARATOR));
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.isBlank(text)) {
			setValue(null);
		}
		else {
			final String trimmed = text.trim();
			final Matcher matcher = acceptedInputPattern.matcher(trimmed);
			if (!matcher.matches()) {
				throw new IllegalArgumentException("Cannot parse integer value: not composed of digits, out of bounds...");
			}
			final int parsed = Integer.valueOf(trimmed.replaceAll(GROUPING_REMOVAL_REGEX, StringUtils.EMPTY));
			setValue(parsed);
		}
	}

	@Override
	public String getAsText() {
		final Number value = (Number) getValue();
		synchronized (NUMBER_FORMAT) {
			return value != null ? NUMBER_FORMAT.format(value) : StringUtils.EMPTY;
		}
	}
}
