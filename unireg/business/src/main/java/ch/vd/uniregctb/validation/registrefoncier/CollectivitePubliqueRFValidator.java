package ch.vd.uniregctb.validation.registrefoncier;

import ch.vd.uniregctb.registrefoncier.CollectivitePubliqueRF;

public class CollectivitePubliqueRFValidator extends TiersRFValidator<CollectivitePubliqueRF> {

	@Override
	protected Class<CollectivitePubliqueRF> getValidatedClass() {
		return CollectivitePubliqueRF.class;
	}
}
