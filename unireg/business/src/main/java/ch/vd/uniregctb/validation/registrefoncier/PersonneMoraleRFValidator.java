package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.PersonneMoraleRF;

public class PersonneMoraleRFValidator extends TiersRFValidator<PersonneMoraleRF> {

	@Override
	protected Class<PersonneMoraleRF> getValidatedClass() {
		return PersonneMoraleRF.class;
	}
}
