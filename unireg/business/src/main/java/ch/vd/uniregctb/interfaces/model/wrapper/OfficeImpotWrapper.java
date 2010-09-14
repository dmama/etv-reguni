package ch.vd.uniregctb.interfaces.model.wrapper;

import java.io.Serializable;

import ch.vd.uniregctb.interfaces.model.OfficeImpot;

public class OfficeImpotWrapper extends CollectiviteAdministrativeWrapper implements OfficeImpot, Serializable {
	
	private static final long serialVersionUID = 264840177280219913L;

	protected OfficeImpotWrapper(ch.vd.infrastructure.model.CollectiviteAdministrative target) {
		super(target);
	}
}
