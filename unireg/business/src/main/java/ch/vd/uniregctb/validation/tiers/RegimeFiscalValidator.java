package ch.vd.uniregctb.validation.tiers;

import org.apache.commons.lang3.StringUtils;

import ch.vd.registre.base.validation.ValidationResults;
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

	@Override
	public ValidationResults validate(RegimeFiscal rf) {
		final ValidationResults vr = super.validate(rf);
		if (!rf.isAnnule()) {
			if (rf.getPortee() == null) {
				vr.addError("La portée est un attribut obligatoire pour un régime fiscal.");
			}
			if (StringUtils.isBlank(rf.getCode())) {
				vr.addError("Le code d'un régime fiscal est un attribut obligatoire.");
			}
		}
		return vr;
	}
}
