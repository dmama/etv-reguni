package ch.vd.uniregctb.validation.tiers;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.AllegementFiscalConfederation;

public class AllegementFiscalConfederationValidator extends AllegementFiscalValidator<AllegementFiscalConfederation> {

	@Override
	protected Class<AllegementFiscalConfederation> getValidatedClass() {
		return AllegementFiscalConfederation.class;
	}

	@Override
	public ValidationResults validate(AllegementFiscalConfederation af) {
		final ValidationResults vr = super.validate(af);
		if (!af.isAnnule()) {
			if (af.getType() == null) {
				vr.addError(String.format("%s %s n'a pas de type d'allègement fixé.", getEntityCategoryName(), getEntityDisplayString(af)));
			}
		}
		return vr;
	}

}
