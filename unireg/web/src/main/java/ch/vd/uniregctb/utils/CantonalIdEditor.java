package ch.vd.uniregctb.utils;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang3.StringUtils;

import ch.vd.uniregctb.common.FormatNumeroHelper;

/**
 * Editeur qui permet d'afficher et de parser les numéros cantonaux (format : xxx-xxx-xxx).
 */
public class CantonalIdEditor extends PropertyEditorSupport {

	@Override
	public String getAsText() {
		final Number cantonalId = (Number) getValue();
		if (cantonalId == null) {
			return "";
		}
		return FormatNumeroHelper.formatCantonalId(String.valueOf(cantonalId));
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (StringUtils.isBlank(text)) {
			setValue(null);
		}
		else {
			final String number = text.replaceAll("[^0-9]", "");
			setValue(Long.valueOf(number));
		}
	}
}
