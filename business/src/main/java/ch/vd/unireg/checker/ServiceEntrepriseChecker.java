package ch.vd.unireg.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.interfaces.entreprise.ServiceEntrepriseRaw;

public class ServiceEntrepriseChecker implements StatusChecker {

	private ServiceEntrepriseRaw serviceEntrepriseRaw;

	@NotNull
	@Override
	public String getName() {
		return "serviceEntreprise";
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
		try {
			serviceEntrepriseRaw.ping();
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceEntrepriseRaw(ServiceEntrepriseRaw serviceEntrepriseRaw) {
		this.serviceEntrepriseRaw = serviceEntrepriseRaw;
	}
}
