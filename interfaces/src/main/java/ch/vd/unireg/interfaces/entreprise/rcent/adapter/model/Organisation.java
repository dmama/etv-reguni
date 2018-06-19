package ch.vd.unireg.interfaces.entreprise.rcent.adapter.model;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.registre.base.date.DateRangeHelper;

public class Organisation {

	/**
	 * Identifiant cantonal
	 */
	private final long cantonalId;

	@NotNull
	private final Map<String,List<DateRangeHelper.Ranged<String>>> organisationIdentifiers;

	private final Map<Long, List<DateRangeHelper.Ranged<Long>>> locations;
	private final List<OrganisationLocation> locationData;

	public Organisation(long cantonalId, @NotNull Map<String, List<DateRangeHelper.Ranged<String>>> organisationIdentifiers,
	                    Map<Long, List<DateRangeHelper.Ranged<Long>>> locations, List<OrganisationLocation> locationData) {
		this.cantonalId = cantonalId;
		this.organisationIdentifiers = organisationIdentifiers;
		this.locations = locations;
		this.locationData = locationData;
	}

	public long getCantonalId() {
		return cantonalId;
	}

	public List<OrganisationLocation> getLocationData() {
		return locationData;
	}

	public Map<Long, List<DateRangeHelper.Ranged<Long>>> getLocations() {
		return locations;
	}

	/**
	 * Historique multivaleur des identifiants de l'entreprises, indexés par catégorie.
	 * @return La Map des identifiants de l'entreprises, ou null si aucun historique.
	 */
	@NotNull
	public Map<String,List<DateRangeHelper.Ranged<String>>> getOrganisationIdentifiers() {
		return organisationIdentifiers;
	}
}