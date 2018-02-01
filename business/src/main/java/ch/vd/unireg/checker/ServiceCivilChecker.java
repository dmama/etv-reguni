package ch.vd.unireg.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;

public class ServiceCivilChecker implements StatusChecker {

	private ServiceCivilRaw serviceCivilRaw;

	@NotNull
	@Override
	public String getName() {
		return "serviceCivil";
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
		try {
			serviceCivilRaw.ping();
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilRaw(ServiceCivilRaw serviceCivilRaw) {
		this.serviceCivilRaw = serviceCivilRaw;
	}
}
