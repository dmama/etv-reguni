package ch.vd.uniregctb.migration.pm.engine.data;

import ch.vd.uniregctb.adapter.rcent.model.Organisation;
import ch.vd.uniregctb.adapter.rcent.model.OrganisationLocation;

/**
 * Container des données civiles retrouvées dans RCEnt pour une organisation et/ou un établissement (= location)
 */
public class DonneesCiviles {

	private final Organisation organisation;
	private final OrganisationLocation location;

	public DonneesCiviles(Organisation organisation, OrganisationLocation location) {
		this.organisation = organisation;
		this.location = location;
	}

	public DonneesCiviles(Organisation organisation) {
		this(organisation, null);
	}

	public Organisation getOrganisation() {
		return organisation;
	}

	public OrganisationLocation getLocation() {
		return location;
	}
}
