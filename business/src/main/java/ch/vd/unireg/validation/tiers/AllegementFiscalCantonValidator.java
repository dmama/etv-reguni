package ch.vd.unireg.validation.tiers;

import ch.vd.unireg.tiers.AllegementFiscalCanton;

public class AllegementFiscalCantonValidator extends AllegementFiscalCantonCommuneValidator<AllegementFiscalCanton> {

	@Override
	protected Class<AllegementFiscalCanton> getValidatedClass() {
		return AllegementFiscalCanton.class;
	}
}
