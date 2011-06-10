package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.securite.model.Operateur;
import ch.vd.uniregctb.interfaces.service.ServiceSecuriteService;

public class ServiceSecuriteChecker implements ServiceChecker {

	private ServiceSecuriteService serviceSecuriteRaw;
	private String details;

	@Override
	public Status getStatus() {
		try {
			Operateur op = serviceSecuriteRaw.getOperateur("zaiptf");
			Assert.isTrue("zaiptf".equalsIgnoreCase(op.getCode()));
			details = null;
			return Status.OK;
		}
		catch (Exception e) {
			details = ExceptionUtils.extractCallStack(e);
			return Status.KO;
		}
	}

	@Override
	public String getStatusDetails() {
		return details;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceSecuriteRaw(ServiceSecuriteService serviceSecuriteRaw) {
		this.serviceSecuriteRaw = serviceSecuriteRaw;
	}
}