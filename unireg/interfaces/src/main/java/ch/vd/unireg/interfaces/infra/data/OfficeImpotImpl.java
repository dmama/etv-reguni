package ch.vd.unireg.interfaces.infra.data;

import java.io.Serializable;

import ch.vd.infrastructure.model.rest.CollectiviteAdministrative;
import ch.vd.unireg.wsclient.host.interfaces.ServiceInfrastructureClient;

public class OfficeImpotImpl extends CollectiviteAdministrativeImpl implements OfficeImpot, Serializable {

	private static final long serialVersionUID = -6974849369927648008L;

	protected OfficeImpotImpl(ch.vd.infrastructure.model.CollectiviteAdministrative target, ch.vd.infrastructure.service.ServiceInfrastructure serviceInfrastructure) {
		super(target, serviceInfrastructure);
	}

	public OfficeImpotImpl(CollectiviteAdministrative target, ServiceInfrastructureClient client) {
		super(target, client);
	}

	protected OfficeImpotImpl(ch.vd.infrastructure.model.CollectiviteAdministrative target) {
		super(target,null);
	}

	public OfficeImpotImpl(CollectiviteAdministrative target) {
		super(target,null);
	}
}
