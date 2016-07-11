package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;

public class ServiceOrganisationChecker implements ServiceChecker {

	private ServiceOrganisationRaw serviceOrganisationRaw;
	private String details;

	@Override
	public Status getStatus() {
		try {
			serviceOrganisationRaw.ping();
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
	public void setServiceOrganisationRaw(ServiceOrganisationRaw serviceOrganisationRaw) {
		this.serviceOrganisationRaw = serviceOrganisationRaw;
	}
}
