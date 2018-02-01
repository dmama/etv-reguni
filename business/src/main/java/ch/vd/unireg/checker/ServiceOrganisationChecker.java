package ch.vd.uniregctb.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;

public class ServiceOrganisationChecker implements StatusChecker {

	private ServiceOrganisationRaw serviceOrganisationRaw;

	@NotNull
	@Override
	public String getName() {
		return "serviceOrganisation";
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
		try {
			serviceOrganisationRaw.ping();
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceOrganisationRaw(ServiceOrganisationRaw serviceOrganisationRaw) {
		this.serviceOrganisationRaw = serviceOrganisationRaw;
	}
}
