package ch.vd.unireg.validation.situationfamille;

import ch.vd.unireg.tiers.SituationFamille;
import ch.vd.unireg.validation.tiers.DateRangeEntityValidator;

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
