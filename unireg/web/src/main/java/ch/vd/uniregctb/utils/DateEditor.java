package ch.vd.uniregctb.utils;

import java.beans.PropertyEditorSupport;
import java.util.Date;

import org.springframework.util.StringUtils;

import ch.vd.registre.base.date.DateHelper;

public class DateEditor extends PropertyEditorSupport {

	private final boolean allowEmpty;

	public DateEditor(boolean allowEmpty) {
		this.allowEmpty = allowEmpty;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {

		if (this.allowEmpty && !StringUtils.hasText(text)) {
			// treat empty String as null value
			setValue(null);
		}
		else {
			setValue(DateHelper.displayStringToDate(text));
		}
	}

	@Override
	public String getAsText() {
		Date value = (Date) getValue();
		return (value != null ? DateHelper.dateToDisplayString(value) : "");
	}

}
