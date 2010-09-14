package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.interfaces.model.CollectiviteAdministrative;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class ServiceInfraChecker implements ServiceChecker {

	private ServiceInfrastructureService serviceInfraRaw;
	private String details;

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

	public String getStatusDetails() {
		return details;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfraRaw(ServiceInfrastructureService serviceInfraRaw) {
		this.serviceInfraRaw = serviceInfraRaw;
	}
}