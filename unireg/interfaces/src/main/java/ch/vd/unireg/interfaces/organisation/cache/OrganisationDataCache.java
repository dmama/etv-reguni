package ch.vd.unireg.interfaces.organisation.cache;

import java.io.Serializable;

import ch.vd.unireg.interfaces.organisation.data.Organisation;

public final class OrganisationDataCache implements Serializable {

	private static final long serialVersionUID = -1423842735882864058L;

	private final Organisation organisation;

	public OrganisationDataCache(Organisation organisation) {
		this.organisation = organisation;
	}

	public Organisation getOrganisation() {
		return organisation;
	}
}
