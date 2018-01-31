package ch.vd.uniregctb.validation.tiers;

import ch.vd.uniregctb.tiers.AllegementFiscalCanton;

public class AllegementFiscalCantonValidator extends AllegementFiscalCantonCommuneValidator<AllegementFiscalCanton> {

	@Override
	protected Class<AllegementFiscalCanton> getValidatedClass() {
		return AllegementFiscalCanton.class;
	}
}
