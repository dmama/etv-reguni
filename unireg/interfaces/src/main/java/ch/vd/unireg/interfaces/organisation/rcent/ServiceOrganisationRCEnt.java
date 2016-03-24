package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.registre.base.date.DateRangeHelper;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationException;
import ch.vd.unireg.interfaces.organisation.ServiceOrganisationRaw;
import ch.vd.unireg.interfaces.organisation.WrongOrganisationReceivedException;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationConstants;
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
	public Identifiers getOrganisationByNoIde(String noide) throws ServiceOrganisationException {
		final ch.vd.uniregctb.adapter.rcent.model.Organisation received = adapter.getOrganisationByNoIde(noide, null);
		if (received == null) {
			return null;
		}

		for (ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation location : received.getLocationData()) {
			final List<DateRangeHelper.Ranged<String>> candidatsIde = location.getIdentifiers().get(OrganisationConstants.CLE_IDE);
			if (candidatsIde != null) {
				for (DateRangeHelper.Ranged<String> candidatIde : candidatsIde) {
					if (noide != null && noide.equals(candidatIde.getPayload())) {
						return new Identifiers(received.getCantonalId(), location.getCantonalId());
					}
				}
			}
		}

		// super bizarre... on nous renvoie des données mais ces données ne contiennent pas l'information recherchée...
		throw new ServiceOrganisationException(String.format("Les données renvoyées par RCEnt pour le numéro IDE %s ne contiennent aucun établissement arborant ce numéro.",
		                                                     noide));
	}

	@Override
	public Map<Long, Organisation> getPseudoOrganisationHistory(long noEvenement) throws ServiceOrganisationException {
		final Map<Long, ch.vd.uniregctb.adapter.rcent.model.Organisation> received = adapter.getPseudoHistoryForEvent(noEvenement);
		if (received == null || received.isEmpty()) {
			return Collections.emptyMap();
		}
		final Map<Long, Organisation> result = new HashMap<>(received.size());
		for (Map.Entry<Long, ch.vd.uniregctb.adapter.rcent.model.Organisation> orgEntry : received.entrySet()) {
			result.put(orgEntry.getKey(), RCEntOrganisationHelper.get(orgEntry.getValue(), infraService));
		}
		return result;
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
