package ch.vd.uniregctb.migration.pm.engine.data;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;

/**
 * Container des données civiles retrouvées dans RCEnt pour une organisation et/ou un établissement (= location)
 */
public class DonneesCiviles {

	private final Organisation organisation;
	private final SiteOrganisation site;

	public DonneesCiviles(Organisation organisation, SiteOrganisation site) {
		this.organisation = organisation;
		this.site = site;
	}

	public DonneesCiviles(Organisation organisation) {
		this(organisation, null);
	}

	public Organisation getOrganisation() {
		return organisation;
	}

	public SiteOrganisation getSite() {
		return site;
	}
}
