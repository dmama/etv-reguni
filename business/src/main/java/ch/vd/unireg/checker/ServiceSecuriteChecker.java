package ch.vd.unireg.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.interfaces.service.ServiceSecuriteException;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;

public class ServiceSecuriteChecker implements StatusChecker {

	public static final String NAME = "serviceSecurite";

	private ServiceSecuriteService serviceSecuriteRaw;

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
			serviceSecuriteRaw.ping();
		}
		catch (ServiceSecuriteException e) {
			throw e;
		}
		catch (Exception e) {
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecuriteRaw(ServiceSecuriteService serviceSecuriteRaw) {
		this.serviceSecuriteRaw = serviceSecuriteRaw;
	}
}