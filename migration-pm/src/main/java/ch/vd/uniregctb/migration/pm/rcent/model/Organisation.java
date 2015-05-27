package ch.vd.uniregctb.migration.pm.rcent.model;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import ch.vd.evd0022.v1.Identifier;
import ch.vd.evd0022.v1.LegalForm;
import ch.vd.evd0022.v1.OrganisationLocation;
import ch.vd.evd0022.v1.SwissMunicipality;
import ch.vd.uniregctb.migration.pm.historizer.container.DateRanged;

public class Organisation {

	/**
	 * Identifiant cantonal
	 */
	private final long cantonalId;

	@NotNull
	private final List<Identifier> organisationIdentifiers;

	@NotNull
	private final List<DateRanged<String>> organisationName;
	private final List<DateRanged<String>> organisationAdditionalName;
	private final List<DateRanged<LegalForm>> legalForm;

	/**
	 * Commune de siège légal
	 */
	private final List<DateRanged<Long>> seat;
	/**
	 * Information détaillée sur les communes de siège. En effet, des changements peuvent survenir
	 * sans pour autant changer le lieu du siège. Il faut donc capturer cette information séparément,
	 * car elle connait potentiellement un plus grand nombre de période.
	 */
	private final List<DateRanged<SwissMunicipality>> seatMunicipalityInfo;

	private final List<DateRanged<OrganisationLocation>> locations;

	private final List<DateRanged<Long>> transferTo;
	private final List<DateRanged<Long>> transferFrom;
	private final List<DateRanged<Long>> replacedBy;
	private final List<DateRanged<Long>> inPreplacementOf;

	public Organisation(long cantonalId,
	                         @NotNull List<Identifier> organisationIdentifiers,
	                         @NotNull List<DateRanged<String>> organisationName,
	                         List<DateRanged<String>> organisationAdditionalName,
	                         List<DateRanged<LegalForm>> legalForm,
	                         List<DateRanged<Long>> seat,
	                         List<DateRanged<SwissMunicipality>> seatMunicipalityInfo, List<DateRanged<OrganisationLocation>> locations,
	                         List<DateRanged<Long>> transferTo,
	                         List<DateRanged<Long>> transferFrom,
	                         List<DateRanged<Long>> replacedBy,
	                         List<DateRanged<Long>> inPreplacementOf) {
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

}