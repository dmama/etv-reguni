package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.unireg.interfaces.organisation.rcent.converters.LegalFormConverter;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class RCEntOrganisationHelper {

	public static Organisation get(ch.vd.uniregctb.adapter.rcent.model.Organisation organisation) {
		return new OrganisationRCEnt(
				RCEntHelper.convert(organisation.getOrganisationIdentifiers()),
				RCEntHelper.convert(organisation.getOrganisationName()),
				RCEntHelper.convert(organisation.getOrganisationAdditionalName()),
				RCEntHelper.convertAndMap(organisation.getLegalForm(), new LegalFormConverter()),
				RCEntHelper.convert(organisation.getLocations()),
				convertLocations(organisation.getLocationData()),
				RCEntHelper.convert(organisation.getTransferTo()),
				RCEntHelper.convert(organisation.getTransferFrom()),
				RCEntHelper.convert(organisation.getReplacedBy()),
				RCEntHelper.convert(organisation.getInReplacementOf())
		);
	}

	private static Map<Long, SiteOrganisation> convertLocations(List<OrganisationLocation> locations) {
		final Map<Long, SiteOrganisation> sites = new HashMap<>();
		for (OrganisationLocation loc : locations) {
			sites.put(loc.getCantonalId(), RCEntSiteOrganisationHelper.get(loc));
		}
		return sites;
	}
}
