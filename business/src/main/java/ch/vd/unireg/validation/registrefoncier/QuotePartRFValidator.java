package ch.vd.unireg.validation.registrefoncier;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.unireg.registrefoncier.QuotePartRF;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

public class QuotePartRFValidator extends DateRangeEntityValidator<QuotePartRF> {
	@Override
	protected Class<QuotePartRF> getValidatedClass() {
		return QuotePartRF.class;
	}

	@Override
	public ValidationResults validate(QuotePartRF entity) {
		return super.validate(entity);
	}

	@Override
	protected String getEntityCategoryName() {
		return "La quote-part RF";
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return true;
	}
}
