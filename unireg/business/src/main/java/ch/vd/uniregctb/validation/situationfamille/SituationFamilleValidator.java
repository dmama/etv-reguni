package ch.vd.uniregctb.validation.situationfamille;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.registre.base.validation.ValidationResults;
import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.validation.EntityValidatorImpl;

/**
 * Validateur des situations de famille
 */
public class SituationFamilleValidator extends EntityValidatorImpl<SituationFamille> {

	@Override
	protected Class<SituationFamille> getValidatedClass() {
		return SituationFamille.class;
	}

	public ValidationResults validate(SituationFamille sf) {
		final ValidationResults results = new ValidationResults();
		if (!sf.isAnnule()) {
			DateRangeHelper.validate(sf, false, true, results);
		}
		return results;
	}
}
