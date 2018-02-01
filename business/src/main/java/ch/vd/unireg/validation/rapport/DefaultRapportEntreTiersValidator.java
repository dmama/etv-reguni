package ch.vd.unireg.validation.rapport;

import ch.vd.unireg.tiers.RapportEntreTiers;

public class DefaultRapportEntreTiersValidator extends RapportEntreTiersValidator<RapportEntreTiers> {

	@Override
	protected Class<RapportEntreTiers> getValidatedClass() {
		return RapportEntreTiers.class;
	}
}
