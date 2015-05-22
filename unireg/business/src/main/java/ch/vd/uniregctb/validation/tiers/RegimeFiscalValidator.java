package ch.vd.uniregctb.validation.tiers;

import ch.vd.uniregctb.tiers.RegimeFiscal;

public class RegimeFiscalValidator extends DateRangeEntityValidator<RegimeFiscal> {

	@Override
	protected String getEntityCategoryName() {
		return "Le régime fiscal";
	}

	@Override
	protected Class<RegimeFiscal> getValidatedClass() {
		return RegimeFiscal.class;
	}
}
