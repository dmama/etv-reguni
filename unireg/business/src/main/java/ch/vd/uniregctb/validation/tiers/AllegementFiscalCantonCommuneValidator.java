package ch.vd.uniregctb.validation.tiers;

import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.AllegementFiscalCantonCommune;

public abstract class AllegementFiscalCantonCommuneValidator<T extends AllegementFiscalCantonCommune> extends AllegementFiscalValidator<T> {

	@Override
	public ValidationResults validate(T af) {
		final ValidationResults vr = super.validate(af);
		if (!af.isAnnule()) {
			if (af.getType() == null) {
				vr.addError(String.format("%s %s n'a pas de type d'allègement fixé.", getEntityCategoryName(), getEntityDisplayString(af)));
			}
		}
		return vr;
	}
}
