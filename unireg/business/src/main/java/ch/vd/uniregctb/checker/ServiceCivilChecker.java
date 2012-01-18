package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

public class ServiceCivilChecker implements ServiceChecker {

	private ServiceCivilService serviceCivilRaw;
	private String details;

	@Override
	public Status getStatus() {
		try {
			Individu individu = serviceCivilRaw.getIndividu(611836, null); // Francis Perroset
			Assert.isEqual(611836L, individu.getNoTechnique());
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
	public void setServiceCivilRaw(ServiceCivilService serviceCivilRaw) {
		this.serviceCivilRaw = serviceCivilRaw;
	}
}
