package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

public class ServiceCivilChecker {

	private ServiceCivilService serviceCivilRaw;
	private String details;

	public Status getStatus() {
		try {
			Individu individu = serviceCivilRaw.getIndividu(611836, 2400); // Francis Perroset
			Assert.isEqual(611836L, individu.getNoTechnique());
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
	public void setServiceCivilRaw(ServiceCivilService serviceCivilRaw) {
		this.serviceCivilRaw = serviceCivilRaw;
	}
}
