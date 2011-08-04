package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureRaw;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class ServiceHostInfraChecker implements ServiceChecker {

	private ServiceInfrastructureRaw serviceInfraRaw;
	private String details;

	@Override
	public Status getStatus() {
		try {
			CollectiviteAdministrative aci = serviceInfraRaw.getCollectivite(ServiceInfrastructureService.noACI);
			Assert.isEqual(ServiceInfrastructureService.noACI, aci.getNoColAdm());
			details = null;
			return Status.OK;
		}
		catch (Exception e) {
			details = ExceptionUtils.extractCallStack(e);
			return Status.KO;
		}
	}

	@Override
	public String getStatusDetails() {
		return details;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfraRaw(ServiceInfrastructureRaw serviceInfraRaw) {
		this.serviceInfraRaw = serviceInfraRaw;
	}
}