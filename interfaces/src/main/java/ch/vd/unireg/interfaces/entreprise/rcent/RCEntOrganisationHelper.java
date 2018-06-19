package ch.vd.unireg.interfaces.entreprise.rcent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivile;
import ch.vd.unireg.interfaces.entreprise.data.EntrepriseCivileRCEnt;
import ch.vd.unireg.interfaces.entreprise.data.EtablissementCivil;
import ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.OrganisationLocation;
import ch.vd.unireg.interfaces.infra.ServiceInfrastructureRaw;

public class RCEntOrganisationHelper {

	public static EntrepriseCivile get(ch.vd.unireg.interfaces.entreprise.rcent.adapter.model.Organisation organisation, ServiceInfrastructureRaw infraService) {
		return new EntrepriseCivileRCEnt(
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
