package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;

public class ServiceInfraChecker implements ServiceChecker {

	private ServiceInfrastructureRaw serviceInfraRaw;
	private String details;

	@Override
	public Status getStatus() {
		try {
			serviceInfraRaw.ping();
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