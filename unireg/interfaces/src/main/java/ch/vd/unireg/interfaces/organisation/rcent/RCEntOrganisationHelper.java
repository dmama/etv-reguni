package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.OrganisationLocation;

public class RCEntOrganisationHelper {

	public static Organisation get(ch.vd.unireg.interfaces.organisation.rcent.adapter.model.Organisation organisation, ServiceInfrastructureRaw infraService) {
		return new OrganisationRCEnt(
				organisation.getCantonalId(),
				RCEntHelper.convert(organisation.getOrganisationIdentifiers()),
				RCEntHelper.convert(organisation.getLocations()),
				convertLocations(organisation.getLocationData(), infraService)
		);
	}

	private static Map<Long, SiteOrganisation> convertLocations(List<OrganisationLocation> locations, ServiceInfrastructureRaw infraService) {
		final Map<Long, SiteOrganisation> sites = new HashMap<>();
		for (OrganisationLocation loc : locations) {
			sites.put(loc.getCantonalId(), RCEntSiteOrganisationHelper.get(loc, infraService));
		}
		return sites;
	}
}
