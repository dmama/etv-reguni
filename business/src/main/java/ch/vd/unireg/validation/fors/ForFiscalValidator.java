package ch.vd.unireg.validation.fors;

import ch.vd.unireg.tiers.ForFiscal;
import ch.vd.unireg.validation.tiers.LocalisationDateeValidator;

/**
 * Classe de base pour les validateurs de fors fiscaux
 */
public abstract class ForFiscalValidator<T extends ForFiscal> extends LocalisationDateeValidator<T> {

	@Override
	protected String getEntityCategoryName() {
		return "Le for fiscal";
	}
}
