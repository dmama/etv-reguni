package ch.vd.uniregctb.utils;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang.StringUtils;

public class EnumEditor extends PropertyEditorSupport {

	private final Class<? extends Enum> enumType;
	private final boolean allowEmpty;

	public EnumEditor(Class<? extends Enum> enumType, boolean allowEmpty) {
		this.enumType = enumType;
		this.allowEmpty = allowEmpty;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (this.allowEmpty && StringUtils.isBlank(text)) {
			setValue(null);
		}
		else {
			final Enum e = Enum.valueOf(enumType, text);
			setValue(e);
		}
	}

	@Override
	public String getAsText() {
		final Enum e = (Enum) getValue();
		return (e != null ? e.name() : "");
	}

}