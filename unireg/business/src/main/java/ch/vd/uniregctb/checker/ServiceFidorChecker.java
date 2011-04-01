package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.interfaces.model.Logiciel;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureRaw;

public class ServiceFidorChecker implements ServiceChecker {

	private ServiceInfrastructureRaw serviceFidorRaw;
	private String details;

	public Status getStatus() {
		try {
			final Logiciel logiciel = serviceFidorRaw.getLogiciel(1L);
			Assert.isEqual("Epsitec SA", logiciel.getFournisseur());
			details = null;
			return Status.OK;
		}
		catch (Exception e) {
			details = ExceptionUtils.extractCallStack(e);
			return Status.KO;
		}
	}

	public String getStatusDetails() {
		return details;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceFidorRaw(ServiceInfrastructureRaw serviceFidorRaw) {
		this.serviceFidorRaw = serviceFidorRaw;
	}
}