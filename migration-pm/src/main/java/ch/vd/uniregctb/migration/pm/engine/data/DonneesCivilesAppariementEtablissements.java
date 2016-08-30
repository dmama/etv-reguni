package ch.vd.uniregctb.migration.pm.engine.data;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import ch.vd.unireg.interfaces.organisation.data.Organisation;
import ch.vd.unireg.interfaces.organisation.data.SiteOrganisation;

public class DonneesCivilesAppariementEtablissements {

	private final Organisation organisation;

	/**
	 * Sites appariés sur des établissements de RegPM (la clé de la map est l'identifiant de l'établissement dans RegPM)
	 */
	private final Map<Long, SiteOrganisation> sites;

	public DonneesCivilesAppariementEtablissements(Organisation organisation, @Nullable Map<Long, SiteOrganisation> sites) {
		this.organisation = organisation;
		this.sites = Optional.ofNullable(sites).orElseGet(Collections::emptyMap);
	}

	public DonneesCivilesAppariementEtablissements(Organisation organisation) {
		this(organisation, null);
	}

	public Organisation getOrganisation() {
		return organisation;
	}

	/**
	 * Sites appariés sur des établissements de RegPM (la clé de la map est l'identifiant de l'établissement dans RegPM)
	 */
	@NotNull
	public Map<Long, SiteOrganisation> getSites() {
		return sites;
	}
}
