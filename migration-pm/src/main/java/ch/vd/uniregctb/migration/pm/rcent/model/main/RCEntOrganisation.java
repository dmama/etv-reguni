package ch.vd.uniregctb.migration.pm.rcent.model.main;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.LegalForm;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntListOfRanges;
import ch.vd.uniregctb.migration.pm.rcent.model.base.RCEntRangedValue;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntIdentification;
import ch.vd.uniregctb.migration.pm.rcent.model.wrappers.RCEntSwissMunicipality;

public class RCEntOrganisation {

	/**
	 * Identifiant cantonal
	 */
	private final long cantonalId;

	/**
	 * Identifiant de l'entreprise
	 */
	@NotNull
	private final RCEntListOfRanges<RCEntIdentification> organisationIdentifier;

	@NotNull
	private final RCEntListOfRanges<RCEntRangedValue<String>> organisationName;
	private final RCEntListOfRanges<RCEntRangedValue<String>> organisationAdditionalName;
	private final RCEntListOfRanges<RCEntRangedValue<LegalForm>> legalForm;

	/**
	 * Commune de siège légal
	 */
	private final RCEntListOfRanges<RCEntSwissMunicipality> seat;

	private final List<RCEntOrganisationLocation> locations;

	private final RCEntListOfRanges<RCEntIdentification> transferTo;
	private final RCEntListOfRanges<RCEntIdentification> transferFrom;
	private final RCEntListOfRanges<RCEntIdentification> replacedBy;
	private final RCEntListOfRanges<RCEntIdentification> inPreplacementOf;


	public RCEntOrganisation(long cantonalId,
	                         @NotNull RCEntListOfRanges<RCEntIdentification> organisationIdentifier,
	                         @NotNull RCEntListOfRanges<RCEntRangedValue<String>> organisationName,
	                         RCEntListOfRanges<RCEntRangedValue<String>> organisationAdditionalName,
	                         RCEntListOfRanges<RCEntRangedValue<LegalForm>> legalForm,
	                         RCEntListOfRanges<RCEntSwissMunicipality> seat, List<RCEntOrganisationLocation> locations,
	                         RCEntListOfRanges<RCEntIdentification> transferTo,
	                         RCEntListOfRanges<RCEntIdentification> transferFrom,
	                         RCEntListOfRanges<RCEntIdentification> replacedBy,
	                         RCEntListOfRanges<RCEntIdentification> inPreplacementOf) {
		this.cantonalId = cantonalId;
		this.organisationIdentifier = organisationIdentifier;
		this.organisationName = organisationName;
		this.organisationAdditionalName = organisationAdditionalName;
		this.legalForm = legalForm;
		this.seat = seat;
		this.locations = locations;
		this.transferTo = transferTo;
		this.transferFrom = transferFrom;
		this.replacedBy = replacedBy;
		this.inPreplacementOf = inPreplacementOf;
	}

	public long getCantonalId() {
		return cantonalId;
	}

	public RCEntListOfRanges<RCEntIdentification> getInPreplacementOf() {
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
	public RCEntListOfRanges<RCEntIdentification> getOrganisationIdentifier() {
		return organisationIdentifier;
	}

	@NotNull
	public RCEntListOfRanges<RCEntRangedValue<String>> getOrganisationName() {
		return organisationName;
	}

	public RCEntListOfRanges<RCEntIdentification> getReplacedBy() {
		return replacedBy;
	}

	public RCEntListOfRanges<RCEntSwissMunicipality> getSeat() {
		return seat;
	}

	public RCEntListOfRanges<RCEntIdentification> getTransferFrom() {
		return transferFrom;
	}

	public RCEntListOfRanges<RCEntIdentification> getTransferTo() {
		return transferTo;
	}
}