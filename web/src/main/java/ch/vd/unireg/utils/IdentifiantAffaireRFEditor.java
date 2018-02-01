package ch.vd.unireg.utils;

import java.beans.PropertyEditorSupport;

import org.apache.commons.lang3.StringUtils;

import ch.vd.unireg.registrefoncier.IdentifiantAffaireRF;

public class IdentifiantAffaireRFEditor extends PropertyEditorSupport {

	private final boolean allowEmpty;
	private final boolean silentParsingError;

	/**
	 * @param allowEmpty         <b>vrai</b> si la donnée peut être nulle; <b>faux</b> pour qu'une donnée nulle lève une erreur.
	 * @param silentParsingError <b>vrai</b> pour qu'une donnée malformée soit interprétée comme nulle; <b>faux</b> pour qu'une erreur soit levée.
	 */
	public IdentifiantAffaireRFEditor(boolean allowEmpty, boolean silentParsingError) {
		this.allowEmpty = allowEmpty;
		this.silentParsingError = silentParsingError;
	}

	@Override
	public void setAsText(String text) throws IllegalArgumentException {

		if (this.allowEmpty && StringUtils.isBlank(text)) {
			// treat empty String as null value
			setValue(null);
		}
		else {
			try {
				final IdentifiantAffaireRF ia = IdentifiantAffaireRF.parse(text);
				setValue(ia);
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


	@Override
	public String getAsText() {
		final IdentifiantAffaireRF value = (IdentifiantAffaireRF) getValue();
		return (value != null ? value.toString() : StringUtils.EMPTY);
	}
}
