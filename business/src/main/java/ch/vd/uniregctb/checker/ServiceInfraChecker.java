package ch.vd.uniregctb.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;

public class ServiceInfraChecker implements StatusChecker {

	private ServiceInfrastructureRaw serviceInfraRaw;

	@NotNull
	@Override
	public String getName() {
		return "serviceInfra";
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
		try {
			serviceInfraRaw.ping();
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceInfraRaw(ServiceInfrastructureRaw serviceInfraRaw) {
		this.serviceInfraRaw = serviceInfraRaw;
	}
}