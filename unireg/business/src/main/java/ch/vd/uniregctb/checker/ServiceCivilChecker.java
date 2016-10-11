package ch.vd.uniregctb.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.ExceptionUtils;
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
			throw new CheckerException(ExceptionUtils.extractCallStack(e));
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceCivilRaw(ServiceCivilRaw serviceCivilRaw) {
		this.serviceCivilRaw = serviceCivilRaw;
	}
}
