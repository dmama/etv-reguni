package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.PersonnePhysiqueRF;

public class PersonnePhysiqueRFValidator extends TiersRFValidator<PersonnePhysiqueRF> {

	@Override
	protected Class<PersonnePhysiqueRF> getValidatedClass() {
		return PersonnePhysiqueRF.class;
	}
}
