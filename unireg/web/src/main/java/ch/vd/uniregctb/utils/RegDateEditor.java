package ch.vd.uniregctb.utils;

import java.beans.PropertyEditorSupport;
import java.text.ParseException;

import org.springframework.util.StringUtils;

import ch.vd.registre.base.date.RegDate;
import ch.vd.registre.base.date.RegDateHelper;

public class RegDateEditor extends PropertyEditorSupport {

	private final boolean allowEmpty;

	private final boolean allowPartial;

	public RegDateEditor(boolean allowEmpty, boolean allowPartial) {
		this.allowEmpty = allowEmpty;
		this.allowPartial = allowPartial;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {

		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// treat empty String as null value
			setValue(null);
		}
		else {
			try {
				setValue(RegDateHelper.displayStringToRegDate(text, allowPartial));
			}
			catch (IllegalArgumentException ex) {
				String message = ex.getMessage();
				message = message.replaceAll("\\{", "[").replaceAll("\\}", "]"); // pour éviter une mauvaise interprétation de la date comme paramètre ({0}) dans le système de traduction de messages.
				throw new IllegalArgumentException(message);
			}
			catch (ParseException ex) {
				throw new IllegalArgumentException("Could not parse date: " + ex.getMessage());
			}
		}
	}

	@Override
	public String getAsText() {

		RegDate value = (RegDate) getValue();
		return (value != null ? RegDateHelper.dateToDisplayString(value) : "");

	}

}
