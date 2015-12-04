package ch.vd.uniregctb.adapter.rcent.model;

import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.LegalForm;
import ch.vd.registre.base.date.DateRangeHelper;

public class Organisation {

	/**
	 * Identifiant cantonal
	 */
	private final long cantonalId;

	@NotNull
	private final Map<String,List<DateRangeHelper.Ranged<String>>> organisationIdentifiers;

	@NotNull
	private final List<DateRangeHelper.Ranged<String>> organisationName;
	private final Map<String, List<DateRangeHelper.Ranged<String>>> organisationAdditionalName;
	private final List<DateRangeHelper.Ranged<LegalForm>> legalForm;

	private final Map<Long, List<DateRangeHelper.Ranged<Long>>> locations;
	private final List<OrganisationLocation> locationData;

	private final Map<Long, List<DateRangeHelper.Ranged<Long>>> transferTo;
	private final Map<Long, List<DateRangeHelper.Ranged<Long>>> transferFrom;
	private final List<DateRangeHelper.Ranged<Long>> replacedBy;
	private final Map<Long, List<DateRangeHelper.Ranged<Long>>> inReplacementOf;

	public Organisation(long cantonalId, @NotNull Map<String, List<DateRangeHelper.Ranged<String>>> organisationIdentifiers,
	                    @NotNull List<DateRangeHelper.Ranged<String>> organisationName, Map<String, List<DateRangeHelper.Ranged<String>>> organisationAdditionalName,
	                    List<DateRangeHelper.Ranged<LegalForm>> legalForm, Map<Long, List<DateRangeHelper.Ranged<Long>>> locations, List<OrganisationLocation> locationData,
	                    Map<Long, List<DateRangeHelper.Ranged<Long>>> transferTo, Map<Long, List<DateRangeHelper.Ranged<Long>>> transferFrom, List<DateRangeHelper.Ranged<Long>> replacedBy,
	                    Map<Long, List<DateRangeHelper.Ranged<Long>>> inReplacementOf) {
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

	public List<DateRangeHelper.Ranged<LegalForm>> getLegalForm() {
		return legalForm;
	}

	public List<OrganisationLocation> getLocationData() {
		return locationData;
	}

	public Map<Long, List<DateRangeHelper.Ranged<Long>>> getLocations() {
		return locations;
	}

	public Map<String, List<DateRangeHelper.Ranged<String>>> getOrganisationAdditionalName() {
		return organisationAdditionalName;
	}

	/**
	 * Historique multivaleur des identifiants de l'entreprises, indexés par catégorie.
	 * @return La Map des identifiants de l'entreprises, ou null si aucun historique.
	 */
	@NotNull
	public Map<String,List<DateRangeHelper.Ranged<String>>> getOrganisationIdentifiers() {
		return organisationIdentifiers;
	}

	@NotNull
	public List<DateRangeHelper.Ranged<String>> getOrganisationName() {
		return organisationName;
	}

	/**
	 * Historique multivaleur des entreprises remplacées, indexés par leur identifiant cantonal.
	 * @return La Map des entreprises remplacées, ou null si aucun historique.
	 */
	public Map<Long, List<DateRangeHelper.Ranged<Long>>> getInReplacementOf() {
		return inReplacementOf;
	}

	public List<DateRangeHelper.Ranged<Long>> getReplacedBy() {
		return replacedBy;
	}

	/**
	 * Historique multivaleur des entreprises reprises, indexés par leur identifiant cantonal.
	 * @return La Map des entreprises reprises, ou null si aucun historique.
	 */
	public Map<Long, List<DateRangeHelper.Ranged<Long>>> getTransferFrom() {
		return transferFrom;
	}

	/**
	 * Historique multivaleur des entreprises reprenantes, indexés par leur identifiant cantonal.
	 * @return La Map des entreprises reprenantes, ou null si aucun historique.
	 */
	public Map<Long, List<DateRangeHelper.Ranged<Long>>> getTransferTo() {
		return transferTo;
	}
}