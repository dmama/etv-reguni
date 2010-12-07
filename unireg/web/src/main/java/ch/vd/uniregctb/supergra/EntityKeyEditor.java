package ch.vd.uniregctb.supergra;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang.StringUtils;

public class EntityKeyEditor extends PropertyEditorSupport {

	private final EntityType type;
	private final boolean allowEmpty;

	public EntityKeyEditor(EntityType type, boolean allowEmpty) {
		this.type = type;
		this.allowEmpty = allowEmpty;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (this.allowEmpty && StringUtils.isBlank(text)) {
			setValue(null);
		}
		else {
			final Long id = Long.valueOf(text.trim());
			setValue(new EntityKey(type, id));
		}
	}

	@Override
	public String getAsText() {
		final EntityKey key = (EntityKey) getValue();
		return (key != null ? key.getId().toString() : "");
	}

}