package ch.vd.unireg.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.interfaces.infra.InfrastructureConnector;

public class ServiceInfraChecker implements StatusChecker {

	public static final String NAME = "serviceInfra";

	private InfrastructureConnector infraConnector;

	@NotNull
	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
		try {
			infraConnector.ping();
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setInfraConnector(InfrastructureConnector infraConnector) {
		this.infraConnector = infraConnector;
	}
}