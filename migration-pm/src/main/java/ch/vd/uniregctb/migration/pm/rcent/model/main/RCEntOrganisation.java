package ch.vd.uniregctb.migration.pm.rcent.model.main;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.LegalForm;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntListOfRanges;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedValue;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntIdentifier;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntSwissMunicipality;

public class RCEntOrganisation {

	/**
	 * Identifiant cantonal
	 */
	private final long cantonalId;

	@NotNull
	private final RCEntListOfRanges<RCEntIdentifier> organisationIdentifiers;

	@NotNull
	private final RCEntListOfRanges<RCEntRangedValue<String>> organisationName;
	private final RCEntListOfRanges<RCEntRangedValue<String>> organisationAdditionalName;
	private final RCEntListOfRanges<RCEntRangedValue<LegalForm>> legalForm;

	/**
	 * Commune de siège légal
	 */
	private final RCEntListOfRanges<RCEntRangedValue<Long>> seat;
	/**
	 * Information détaillée sur les communes de siège. En effet, des changements peuvent survenir
	 * sans pour autant changer le lieu du siège. Il faut donc capturer cette information séparément,
	 * car elle connait potentiellement un plus grand nombre de période.
	 */
	private final RCEntListOfRanges<RCEntSwissMunicipality> seatMunicipalityInfo;

	private final List<RCEntOrganisationLocation> locations;

	private final RCEntListOfRanges<RCEntRangedValue<Long>> transferTo;
	private final RCEntListOfRanges<RCEntRangedValue<Long>> transferFrom;
	private final RCEntListOfRanges<RCEntRangedValue<Long>> replacedBy;
	private final RCEntListOfRanges<RCEntRangedValue<Long>> inPreplacementOf;

	public RCEntOrganisation(long cantonalId,
	                         @NotNull RCEntListOfRanges<RCEntIdentifier> organisationIdentifiers,
	                         @NotNull RCEntListOfRanges<RCEntRangedValue<String>> organisationName,
	                         RCEntListOfRanges<RCEntRangedValue<String>> organisationAdditionalName,
	                         RCEntListOfRanges<RCEntRangedValue<LegalForm>> legalForm,
	                         RCEntListOfRanges<RCEntRangedValue<Long>> seat,
	                         RCEntListOfRanges<RCEntSwissMunicipality> seatMunicipalityInfo, List<RCEntOrganisationLocation> locations,
	                         RCEntListOfRanges<RCEntRangedValue<Long>> transferTo,
	                         RCEntListOfRanges<RCEntRangedValue<Long>> transferFrom,
	                         RCEntListOfRanges<RCEntRangedValue<Long>> replacedBy,
	                         RCEntListOfRanges<RCEntRangedValue<Long>> inPreplacementOf) {
		this.cantonalId = cantonalId;
		this.organisationIdentifiers = organisationIdentifiers;
		this.organisationName = organisationName;
		this.organisationAdditionalName = organisationAdditionalName;
		this.legalForm = legalForm;
		this.seat = seat;
		this.seatMunicipalityInfo = seatMunicipalityInfo;
		this.locations = locations;
		this.transferTo = transferTo;
		this.transferFrom = transferFrom;
		this.replacedBy = replacedBy;
		this.inPreplacementOf = inPreplacementOf;
	}

	public long getCantonalId() {
		return cantonalId;
	}

	public RCEntListOfRanges<RCEntRangedValue<Long>> getInPreplacementOf() {
		return inPreplacementOf;
	}

	public RCEntListOfRanges<RCEntRangedValue<LegalForm>> getLegalForm() {
		return legalForm;
	}

	public List<RCEntOrganisationLocation> getLocations() {
		return locations;
	}

	public RCEntListOfRanges<RCEntRangedValue<String>> getOrganisationAdditionalName() {
		return organisationAdditionalName;
	}

	@NotNull
	public RCEntListOfRanges<RCEntIdentifier> getOrganisationIdentifiers() {
		return organisationIdentifiers;
	}

	@NotNull
	public RCEntListOfRanges<RCEntRangedValue<String>> getOrganisationName() {
		return organisationName;
	}

	public RCEntListOfRanges<RCEntRangedValue<Long>> getReplacedBy() {
		return replacedBy;
	}

	public RCEntListOfRanges<RCEntRangedValue<Long>> getSeat() {
		return seat;
	}

	public RCEntListOfRanges<RCEntSwissMunicipality> getSeatMunicipalityInfo() {
		return seatMunicipalityInfo;
	}

	public RCEntListOfRanges<RCEntRangedValue<Long>> getTransferFrom() {
		return transferFrom;
	}

	public RCEntListOfRanges<RCEntRangedValue<Long>> getTransferTo() {
		return transferTo;
	}
}