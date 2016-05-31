package ch.vd.uniregctb.utils;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang3.StringUtils;

/**
 * Editeur spécialisé pour les numéros d'individu.
 */
public class IndividuNumberEditor extends PropertyEditorSupport {

	private final boolean allowEmpty;

	public IndividuNumberEditor(boolean allowEmpty) {
		this.allowEmpty = allowEmpty;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {
		if (this.allowEmpty && StringUtils.isBlank(text)) {
			// treat empty String as null value
			setValue(null);
		}
		else {
			// on ignore tous les caractères non-numériques
			text = text.replaceAll("[^\\d]", "");
			try {
				final Long id = Long.valueOf(text);
				setValue(id);
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException("Impossible de lire le numéro d'individu: " + text);
			}
		}
	}

	@Override
	public String getAsText() {
		final Long id = (Long) getValue();
		return (id == null ? "" : String.format("%d", id));
	}
}
