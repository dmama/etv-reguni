package ch.vd.unireg.interfaces.organisation.rcent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import ch.vd.unireg.interfaces.organisation.data.FormeLegale;
import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

public class RCEntOrganisationConverter {

	public static Organisation get(ch.vd.uniregctb.adapter.rcent.model.Organisation organisation) {
		return new Organisation(
				organisation.getCantonalId(),
				RCEntHelper.convert(organisation.getOrganisationIdentifiers()),
				RCEntHelper.convert(organisation.getOrganisationName()),
				RCEntHelper.convert(organisation.getOrganisationAdditionalName()),
				RCEntHelper.convertAndMap(organisation.getLegalForm(), new Function<ch.vd.evd0022.v1.LegalForm, FormeLegale>() {
					@Override
					public FormeLegale apply(ch.vd.evd0022.v1.LegalForm legalForm) {
						return FormeLegale.valueOf(legalForm.toString());
					}
				}),
				RCEntHelper.convert(organisation.getLocations()),
				convertLocations(organisation.getLocationData()),
				RCEntHelper.convert(organisation.getTransferTo()),
				RCEntHelper.convert(organisation.getTransferFrom()),
				RCEntHelper.convert(organisation.getReplacedBy()),
				RCEntHelper.convert(organisation.getInReplacementOf())
		);
	}

	private static List<SiteOrganisation> convertLocations(List<OrganisationLocation> locations) {
		List<SiteOrganisation> sites = new ArrayList<>();
		for (OrganisationLocation loc : locations) {
			sites.add(RCEntSiteOrganisationConverter.get(loc));
		}
		return sites;
	}
}
