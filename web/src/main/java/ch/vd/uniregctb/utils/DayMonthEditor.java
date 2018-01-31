package ch.vd.uniregctb.utils;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.type.DayMonth;
import ch.vd.uniregctb.type.DayMonthHelper;

public class DayMonthEditor extends PropertyEditorSupport {

	private final boolean allowEmpty;
	private final boolean silentParsingError;
	private final DayMonthHelper.StringFormat format;

	/**
	 * @param allowEmpty         <b>vrai</b> si la donnée peut être nulle; <b>faux</b> pour qu'une donnée nulle lève une erreur.
	 * @param silentParsingError <b>vrai</b> pour qu'une donnée malformée soit interprétée comme nulle; <b>faux</b> pour qu'une erreur soit levée.
	 */
	public DayMonthEditor(boolean allowEmpty, boolean silentParsingError) {
		this(allowEmpty, silentParsingError, DayMonthHelper.StringFormat.DISPLAY);
	}

	/**
	 * @param allowEmpty         <b>vrai</b> si la donnée peut être nulle; <b>faux</b> pour qu'une donnée nulle lève une erreur.
	 * @param silentParsingError <b>vrai</b> pour qu'une donnée malformée soit interprétée comme nulle; <b>faux</b> pour qu'une erreur soit levée.
	 * @param format             le format souhaité
	 */
	public DayMonthEditor(boolean allowEmpty, boolean silentParsingError, DayMonthHelper.StringFormat format) {
		this.allowEmpty = allowEmpty;
		this.silentParsingError = silentParsingError;
		this.format = format;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {

		if (this.allowEmpty && StringUtils.isBlank(text)) {
			// treat empty String as null value
			setValue(null);
		}
		else {
			try {
				final DayMonth dm = parseDayMonth(text);
				setValue(dm);
			}
			catch (IllegalArgumentException e) {
				if (silentParsingError) {
					setValue(null);
				}
				else {
					throw e;
				}
			}
		}
	}

	private DayMonth parseDayMonth(String text) {
		try {
			return format.fromString(text);
		}
		catch (IllegalArgumentException ex) {
			String message = ex.getMessage();
			message = message.replaceAll("\\{", "[").replaceAll("\\}", "]"); // pour éviter une mauvaise interprétation de la date comme paramètre ({0}) dans le système de traduction de messages.
			throw new IllegalArgumentException(message);
		}
		catch (ParseException ex) {
			throw new IllegalArgumentException("Could not parse day-month: " + ex.getMessage(), ex);
		}
	}

	@Override
	public String getAsText() {
		final DayMonth value = (DayMonth) getValue();
		return (value != null ? format.toString(value) : StringUtils.EMPTY);
	}
}
