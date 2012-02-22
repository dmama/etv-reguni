package ch.vd.uniregctb.interfaces.service.rcpers;

import ch.vd.evd0006.v1.Event;
import ch.vd.unireg.wsclient.rcpers.RcPersClient;
import ch.vd.uniregctb.interfaces.model.Individu;
import ch.vd.uniregctb.interfaces.model.impl.IndividuRCPers;
import ch.vd.uniregctb.interfaces.service.ServiceInfrastructureService;

public class RcPersClientHelperImpl implements RcPersClientHelper {

	private RcPersClient rcPersClient;
	private ServiceInfrastructureService infraService;

	@SuppressWarnings("UnusedDeclaration")
	public void setRcPersClient(RcPersClient rcPersClient) {
		this.rcPersClient = rcPersClient;
	}

	@SuppressWarnings("UnusedDeclaration")
	public void setInfraService(ServiceInfrastructureService infraService) {
		this.infraService = infraService;
	}

	@Override
	public Individu getIndividuFromEvent(long eventId) {
		final Event ref = rcPersClient.getEvent(eventId);
		if (ref != null) {
			final Individu individu = IndividuRCPers.get(ref.getPersonAfterEvent(), null, infraService);
			if (individu != null) {
				return individu;
			}
		}
		return null;
	}
}
