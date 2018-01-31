package ch.vd.uniregctb.validation.fors;

import ch.vd.uniregctb.tiers.ForFiscal;
import ch.vd.uniregctb.validation.tiers.LocalisationDateeValidator;

/**
 * Classe de base pour les validateurs de fors fiscaux
 */
public abstract class ForFiscalValidator<T extends ForFiscal> extends LocalisationDateeValidator<T> {

	@Override
	protected String getEntityCategoryName() {
		return "Le for fiscal";
	}
}
