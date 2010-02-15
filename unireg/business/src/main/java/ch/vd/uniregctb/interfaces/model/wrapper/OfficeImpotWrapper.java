package ch.vd.uniregctb.interfaces.model.wrapper;

import ch.vd.uniregctb.interfaces.model.OfficeImpot;

public class OfficeImpotWrapper extends CollectiviteAdministrativeWrapper implements OfficeImpot {

	protected OfficeImpotWrapper(ch.vd.infrastructure.model.CollectiviteAdministrative target) {
		super(target);
	}
}
