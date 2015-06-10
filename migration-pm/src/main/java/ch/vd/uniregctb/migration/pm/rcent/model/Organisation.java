package ch.vd.uniregctb.migration.pm.rcent.model;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.LegalForm;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;

public class Organisation {

	/**
	 * Identifiant cantonal
	 */
	private final long cantonalId;

	@NotNull
	private final Map<String,List<DateRanged<String>>> organisationIdentifiers;

	@NotNull
	private final List<DateRanged<String>> organisationName;
	private final List<DateRanged<String>> organisationAdditionalName;
	private final List<DateRanged<LegalForm>> legalForm;

	private final List<DateRanged<Long>> locations;
	private final List<OrganisationLocation> locationData;

	private final List<DateRanged<Long>> transferTo;
	private final List<DateRanged<Long>> transferFrom;
	private final List<DateRanged<Long>> replacedBy;
	private final List<DateRanged<Long>> inReplacementOf;

	public Organisation(long cantonalId, @NotNull Map<String,List<DateRanged<String>>> organisationIdentifiers,
	                    @NotNull List<DateRanged<String>> organisationName, List<DateRanged<String>> organisationAdditionalName,
	                    List<DateRanged<LegalForm>> legalForm, List<DateRanged<Long>> locations, List<OrganisationLocation> locationData,
	                    List<DateRanged<Long>> transferTo, List<DateRanged<Long>> transferFrom, List<DateRanged<Long>> replacedBy,
	                    List<DateRanged<Long>> inReplacementOf) {
		this.cantonalId = cantonalId;
		this.organisationIdentifiers = organisationIdentifiers;
		this.organisationName = organisationName;
		this.organisationAdditionalName = organisationAdditionalName;
		this.legalForm = legalForm;
		this.locations = locations;
		this.locationData = locationData;
		this.transferTo = transferTo;
		this.transferFrom = transferFrom;
		this.replacedBy = replacedBy;
		this.inReplacementOf = inReplacementOf;
	}

	public long getCantonalId() {
		return cantonalId;
	}

	public List<DateRanged<Long>> getInReplacementOf() {
		return inReplacementOf;
	}

	public List<DateRanged<LegalForm>> getLegalForm() {
		return legalForm;
	}

	public List<OrganisationLocation> getLocationData() {
		return locationData;
	}

	public List<DateRanged<Long>> getLocations() {
		return locations;
	}

	public List<DateRanged<String>> getOrganisationAdditionalName() {
		return organisationAdditionalName;
	}

	@NotNull
	public Map<String,List<DateRanged<String>>> getOrganisationIdentifiers() {
		return organisationIdentifiers;
	}

	@NotNull
	public List<DateRanged<String>> getOrganisationName() {
		return organisationName;
	}

	public List<DateRanged<Long>> getReplacedBy() {
		return replacedBy;
	}

	public List<DateRanged<Long>> getTransferFrom() {
		return transferFrom;
	}

	public List<DateRanged<Long>> getTransferTo() {
		return transferTo;
	}
}