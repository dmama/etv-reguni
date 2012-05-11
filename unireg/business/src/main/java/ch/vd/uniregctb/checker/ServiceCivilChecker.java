package ch.vd.uniregctb.checker;

import ch.vd.registre.base.utils.Assert;
import ch.vd.registre.base.utils.ExceptionUtils;
import ch.vd.unireg.interfaces.civil.ServiceCivilRaw;
import ch.vd.unireg.interfaces.civil.data.Individu;

public class ServiceCivilChecker implements ServiceChecker {

	private ServiceCivilRaw serviceCivilRaw;
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
	public void setServiceCivilRaw(ServiceCivilRaw serviceCivilRaw) {
		this.serviceCivilRaw = serviceCivilRaw;
	}
}
