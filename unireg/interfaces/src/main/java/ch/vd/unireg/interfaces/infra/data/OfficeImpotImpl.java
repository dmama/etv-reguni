package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

public class OfficeImpotImpl extends CollectiviteAdministrativeImpl implements OfficeImpot, Serializable {

	private static final long serialVersionUID = -6974849369927648008L;

	protected OfficeImpotImpl(ch.vd.infrastructure.model.CollectiviteAdministrative target, ch.vd.infrastructure.service.ServiceInfrastructure serviceInfrastructure) {
		super(target, serviceInfrastructure);
	}
}
