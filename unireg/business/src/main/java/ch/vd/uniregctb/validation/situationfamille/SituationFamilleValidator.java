package ch.vd.uniregctb.validation.situationfamille;

import ch.vd.uniregctb.tiers.SituationFamille;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

/**
 * Validateur des situations de famille
 */
public class SituationFamilleValidator extends DateRangeEntityValidator<SituationFamille> {

	@Override
	protected Class<SituationFamille> getValidatedClass() {
		return SituationFamille.class;
	}

	@Override
	protected String getEntityCategoryName() {
		return "La situation de famille";
	}
}
