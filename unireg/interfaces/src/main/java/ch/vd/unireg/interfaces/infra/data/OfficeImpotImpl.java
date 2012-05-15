package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public class OfficeImpotImpl extends CollectiviteAdministrativeImpl implements OfficeImpot, Serializable {

	private static final long serialVersionUID = -7715907113394668313L;

	protected OfficeImpotImpl(ch.vd.infrastructure.model.CollectiviteAdministrative target) {
		super(target);
	}
}
