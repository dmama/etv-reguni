package ch.vd.uniregctb.utils;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang.StringUtils;

import ch.vd.uniregctb.common.FormatNumeroHelper;
import ch.vd.uniregctb.tiers.Contribuable;

/**
 * Editeur spécialisé pour les numéros de tiers/contribuable.
 */
public class TiersNumberEditor extends PropertyEditorSupport {

	private final boolean allowEmpty;

	public TiersNumberEditor(boolean allowEmpty) {
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
				if (id > Contribuable.CTB_GEN_LAST_ID) {
					throw new IllegalArgumentException("Le numéro de tiers " + text + " est plus grand que le maximum autorisé");
				}
				setValue(id);
			}
			catch (NumberFormatException e) {
				throw new IllegalArgumentException("Impossible de lire le numéro de tiers: " + text);
			}
		}
	}

	@Override
	public String getAsText() {
		final Long id = (Long) getValue();
		return (id == null ? "" : FormatNumeroHelper.numeroCTBToDisplay(id));
	}
}
