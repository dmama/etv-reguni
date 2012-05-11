package ch.vd.uniregctb.interfaces.service.rcpers;

import ch.vd.unireg.interfaces.civil.data.IndividuApresEvenement;
import ch.vd.uniregctb.interfaces.service.ServiceCivilService;

public class RcPersClientHelperImpl implements RcPersClientHelper {

	private ServiceCivilService serviceCivil;

	public void setServiceCivil(ServiceCivilService serviceCivil) {
		this.serviceCivil = serviceCivil;
	}

	@Override
	public IndividuApresEvenement getIndividuFromEvent(long eventId) {
		return serviceCivil.getIndividuFromEvent(eventId);
	}
}
