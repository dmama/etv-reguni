package ch.vd.unireg.interfaces.organisation.rcent;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.WrongOrganisationReceivedException;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.wsclient.rcent.RcEntClient;
import ch.vd.unireg.wsclient.rcent.RcEntClientException;
import ch.vd.uniregctb.adapter.rcent.service.RCEntAdapter;

public class ServiceOrganisationRCEnt implements ServiceOrganisationRaw {

	private final RcEntClient client;
	private final RCEntAdapter adapter;
	private final ServiceInfrastructureRaw infraService;

	public ServiceOrganisationRCEnt(RCEntAdapter adapter, RcEntClient client, ServiceInfrastructureRaw infraService) {
		this.adapter = adapter;
		this.client = client;
		this.infraService = infraService;
	}

	@Override
	public Organisation getOrganisationHistory(long noOrganisation) throws ServiceOrganisationException {
		final ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getOrganisationHistory(noOrganisation);
		if (received == null) {
			return null;
		}
		sanityCheck(noOrganisation, received.getCantonalId());
		return RCEntOrganisationHelper.get(received, infraService);
	}

	@Override
	public Long getOrganisationPourSite(Long noSite) throws ServiceOrganisationException {
		final ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getLocation(noSite);
		if (received == null) {
			return null;
		}
		return received.getCantonalId();
	}

	@Override
	public void ping() throws ServiceOrganisationException {
		try {
			client.ping();
		}
		catch (RcEntClientException e) {
			throw new ServiceOrganisationException(e);
		}
	}

	private void sanityCheck(long noOrganisation, long receivedId) throws ServiceOrganisationException {
		if (receivedId != noOrganisation) {
			throw new WrongOrganisationReceivedException(noOrganisation, receivedId);
		}
	}
}
