package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.infra.data.Logiciel;

public class ServiceFidorChecker implements ServiceChecker {

	private ServiceInfrastructureRaw serviceFidorRaw;
	private String details;

	@Override
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

	@Override
	public String getStatusDetails() {
		return details;
	}

	@SuppressWarnings({"UnusedDeclaration"})
	public void setServiceFidorRaw(ServiceInfrastructureRaw serviceFidorRaw) {
		this.serviceFidorRaw = serviceFidorRaw;
	}
}