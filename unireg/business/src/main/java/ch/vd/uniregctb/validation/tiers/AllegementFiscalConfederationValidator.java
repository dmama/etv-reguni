package ch.vd.uniregctb.validation.tiers;

import ch.vd.uniregctb.tiers.AllegementFiscalConfederation;

public class AllegementFiscalConfederationValidator extends AllegementFiscalValidator<AllegementFiscalConfederation> {

	@Override
	protected Class<AllegementFiscalConfederation> getValidatedClass() {
		return AllegementFiscalConfederation.class;
	}

}
