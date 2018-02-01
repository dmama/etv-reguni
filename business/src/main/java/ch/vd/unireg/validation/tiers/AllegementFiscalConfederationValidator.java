package ch.vd.unireg.validation.tiers;

import ch.vd.unireg.tiers.AllegementFiscalConfederation;

public class AllegementFiscalConfederationValidator extends AllegementFiscalValidator<AllegementFiscalConfederation> {

	@Override
	protected Class<AllegementFiscalConfederation> getValidatedClass() {
		return AllegementFiscalConfederation.class;
	}

}
