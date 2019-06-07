package ch.vd.unireg.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.interfaces.civil.IndividuConnector;

public class ServiceCivilChecker implements StatusChecker {

	public static final String NAME = "serviceCivil";

	private IndividuConnector individuConnector;

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
			individuConnector.ping();
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setIndividuConnector(IndividuConnector individuConnector) {
		this.individuConnector = individuConnector;
	}
}
