package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;
import ch.vd.unireg.interfaces.organisation.data.EtablissementCivil;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.OrganisationRCEnt;
import ch.vd.unireg.interfaces.organisation.rcent.adapter.model.OrganisationLocation;

public class RCEntOrganisationHelper {

	public static Organisation get(ch.vd.unireg.interfaces.organisation.rcent.adapter.model.Organisation organisation, ServiceInfrastructureRaw infraService) {
		return new OrganisationRCEnt(
				organisation.getCantonalId(),
				RCEntHelper.convert(organisation.getLocations()),
				convertLocations(organisation.getLocationData(), infraService)
		);
	}

	private static Map<Long, EtablissementCivil> convertLocations(List<OrganisationLocation> locations, ServiceInfrastructureRaw infraService) {
		final Map<Long, EtablissementCivil> etablissements = new HashMap<>();
		for (OrganisationLocation loc : locations) {
			etablissements.put(loc.getCantonalId(), RCEntEtablissementHelper.get(loc, infraService));
		}
		return etablissements;
	}
}
