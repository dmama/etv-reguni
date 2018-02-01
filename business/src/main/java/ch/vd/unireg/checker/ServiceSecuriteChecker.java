package ch.vd.unireg.checker;

import org.jetbrains.annotations.NotNull;

import ch.vd.shared.statusmanager.CheckerException;
import ch.vd.shared.statusmanager.StatusChecker;
import ch.vd.unireg.interfaces.service.ServiceSecuriteService;
import ch.vd.unireg.interfaces.service.host.Operateur;

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
			throw new CheckerException(e.getMessage());
		}
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecuriteRaw(ServiceSecuriteService serviceSecuriteRaw) {
		this.serviceSecuriteRaw = serviceSecuriteRaw;
	}
}