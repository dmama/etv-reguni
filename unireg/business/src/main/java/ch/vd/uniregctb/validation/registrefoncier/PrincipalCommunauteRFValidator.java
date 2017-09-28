package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.PrincipalCommunauteRF;
import ch.vd.uniregctb.validation.tiers.DateRangeEntityValidator;

public class PrincipalCommunauteRFValidator extends DateRangeEntityValidator<PrincipalCommunauteRF> {
	@Override
	protected String getEntityCategoryName() {
		return "Le principal du modèle de communauté RF";
	}

	@Override
	protected Class<PrincipalCommunauteRF> getValidatedClass() {
		return PrincipalCommunauteRF.class;
	}

	@Override
	protected boolean isDateDebutNullAllowed() {
		return false;
	}
}
