package ch.vd.uniregctb.utils;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;

import org.springframework.util.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RegDateEditor extends PropertyEditorSupport {

	private final boolean allowEmpty;
	private final boolean allowPartial;
	private final boolean silentParsingError;

	/**
	 * @param allowEmpty         <b>vrai</b> si la date peut être nulle; <b>faux</b> pour qu'une date nulle lève une erreur.
	 * @param allowPartial       <b>vrai</b> si la date peut être partielle; <b>faux</b> pour qu'une date partielle lève une erreur.
	 * @param silentParsingError <b>vrai</b> pour qu'une date malformée soit interprétée comme nulle; <b>faux</b> pour qu'une erreur soit levée.
	 */
	public RegDateEditor(boolean allowEmpty, boolean allowPartial, boolean silentParsingError) {
		this.allowEmpty = allowEmpty;
		this.allowPartial = allowPartial;
		this.silentParsingError = silentParsingError;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {

		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// treat empty String as null value
			setValue(null);
		}
		else {
			try {
				final RegDate date = parseDate(text);
				setValue(date);
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

	private RegDate parseDate(String text) {
		try {
			return RegDateHelper.displayStringToRegDate(text, allowPartial);
		}
		catch (IllegalArgumentException ex) {
			String message = ex.getMessage();
			message = message.replaceAll("\\{", "[").replaceAll("\\}", "]"); // pour éviter une mauvaise interprétation de la date comme paramètre ({0}) dans le système de traduction de messages.
			throw new IllegalArgumentException(message);
		}
		catch (ParseException ex) {
			throw new IllegalArgumentException("Could not parse date: " + ex.getMessage(), ex);
		}
	}

	@Override
	public String getAsText() {
		final RegDate value = (RegDate) getValue();
		return (value != null ? RegDateHelper.dateToDisplayString(value) : "");
	}
}
