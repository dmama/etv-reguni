package ch.vd.uniregctb.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;
import ch.vd.uniregctb.interfaces.service.host.Operateur;

public class ServiceSecuriteChecker implements StatusChecker {

	private ServiceSecuriteService serviceSecuriteRaw;

	@NotNull
	@Override
	public String getName() {
		return "serviceSecurite";
	}

	@Override
	public int getTimeout() {
		return 1000;
	}

	@Override
	public void check() throws CheckerException {
		try {
			Operateur op = serviceSecuriteRaw.getOperateur("zaiptf");
			if (op == null) {
				throw new CheckerException("Impossible de trouver l'opérateur zaiptf");
			}
			if (!"zaiptf".equalsIgnoreCase(op.getCode())) {
				throw new CheckerException("Données incohérentes retournées");
			}
		}
		catch (CheckerException e) {
			throw e;
		}
		catch (Exception e) {
			throw new CheckerException(ExceptionUtils.extractCallStack(e));
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecuriteRaw(ServiceSecuriteService serviceSecuriteRaw) {
		this.serviceSecuriteRaw = serviceSecuriteRaw;
	}
}