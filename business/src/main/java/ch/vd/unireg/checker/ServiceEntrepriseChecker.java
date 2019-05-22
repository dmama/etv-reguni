package ch.vd.unireg.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.interfaces.entreprise.EntrepriseConnector;

public class ServiceEntrepriseChecker implements StatusChecker {

	private EntrepriseConnector entrepriseConnector;

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
			entrepriseConnector.ping();
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setEntrepriseConnector(EntrepriseConnector entrepriseConnector) {
		this.entrepriseConnector = entrepriseConnector;
	}
}
