package ch.vd.uniregctb.validation.rapport;

import ch.vd.uniregctb.tiers.RapportEntreTiers;

public class DefaultRapportEntreTiersValidator extends RapportEntreTiersValidator<RapportEntreTiers> {

	@Override
	protected Class<RapportEntreTiers> getValidatedClass() {
		return RapportEntreTiers.class;
	}
}
